#!/bin/bash
set -eu +bm -o pipefail
IFS=$'\n\t'

# Defaults
confluence_logfile="test-confluence.log"
timeout_startup=1800
timeout_shutdown=300
timeout_test=900

confluence_pid=0
conflunece_input_pid=0

confluence_input_pipe=$(mktemp /tmp/ConfluenceInputPipeXXXX)
rm -f "$confluence_input_pipe"
mkfifo "$confluence_input_pipe"

function timestamp {
    date +%s
}

function shutdown_confluence {
    set +e

    # Try to shutdown confluence gracefully by sending EOF to process.
    if [ ! "$confluence_pid" -eq 0 ]; then
        echo -n "[INFO] shutting down confluence "
        kill "$conflunece_input_pid" >/dev/null 2>&1
        wait "$conflunece_input_pid" >/dev/null 2>&1
        echo -ne "\x04" > "$confluence_input_pipe"
        stoptime=$(timestamp)
        while [ $(expr "$(timestamp)" - "$stoptime" ) -lt "$timeout_shutdown" ] && kill -0 $confluence_pid >/dev/null 2>&1;  do
            sleep 10
            echo -n "."
        done

        if kill -0 $confluence_pid >/dev/null 2>&1; then
            echo ""
            echo "[ERROR] failed to stop confluence gracefully. Forcing shutdown."
            kill -9 $confluence_pid
        else
            echo " done"
        fi
    fi

    rm -f "$confluence_input_pipe"
}

# Print help
function help {
    echo "Setup confluence and run integration tests."
    echo ""
    echo "Usage: ./integration-test.sh [Options]"
    echo ""
    echo "Options:"
    echo "  -version {version}          confluence version to use, defaults to the version in pom.xml"
    echo "  -timeout-startup {seconds}  timeout for confluence startup in seconds, defaults to 1800"
    echo "  -timeout-shutdown {seconds} timeout for confluence shutdown in seconds, defaults to 300"
    echo "  -timeout-test {seconds}     timeout for integration test, defaults to 900"
    echo "  -confluence-logfile {path}  path to logfile for confluence, defaults to test-confluence.log"
    echo "  -help                       print this help"
    echo ""
    echo "Exit codes:"
    echo "  0 – everything is OK"
    echo "  1 - something went wrong"
    echo "  2 – invalid arguments"
    echo "  4 - timeout startup"
    echo "  5 - test failed"
    echo ""
    echo "Example: ./integration-test.sh -version 6.8.0 -timeout-startup 500 -timeout-test 1000"
}

integration_test_status=0

function run_integration_test {
    # Run integration test
    test="$1"
    echo "[INFO] running test $test"
    response=$(curl -f -u admin:admin -H "Accept: application/xml" "http://127.0.0.1:1990/confluence/rest/mail2blog-tests/1.0/runtest/$test")

    if echo $response | grep '<successful>true</successful>' > /dev/null 2>&1; then
        echo "[INFO] test $test was successful"
    else
        echo "[ERROR] test $test failed"
        integration_test_status=5
        echo "$response"
    fi
}

# Shutdown confluence on exit
trap shutdown_confluence EXIT

# Flags for atlassian-run and atlassian-clean
flags=""

# Extract args
version_flag=0
timeout_startup_flag=0
timeout_shutdown_flag=0
timeout_test_flag=0
confluence_logfile_flag=0
for arg in "$@"; do
    if [ "$version_flag" -eq 1 ]; then
        version_flag=0
        flags="$flags -Dconfluence.version=$arg -Dconfluence.data.version=$arg"
    elif [ "$timeout_startup_flag" -eq 1 ]; then
        timeout_startup_flag=0
        timeout_startup="$arg"
    elif [ "$timeout_shutdown_flag" -eq 1 ]; then
        timeout_shutdown_flag=0
        timeout_shutdown="$arg"
    elif [ "$timeout_test_flag" -eq 1 ]; then
        timeout_test_flag=0
        timeout_test="$arg"
    elif [ "$confluence_logfile_flag" -eq 1 ]; then
        confluence_logfile_flag=0
        confluence_logfile="$arg"
    else
        if [ "$arg" == '-version' ]; then
            version_flag=1
        elif [ "$arg" == '-timeout-startup' ]; then
            timeout_startup_flag=1
        elif [ "$arg" == '-timeout-shutdown' ]; then
            timeout_shutdown_flag=1
        elif [ "$arg" == '-timeout-test' ]; then
            timeout_test_flag=1
        elif [ "$arg" == '-confluence-logfile' ]; then
            confluence_logfile_flag=1
        elif [ "$arg" == '-help' ]; then
            help
            exit 0
        else
            echo "[ERROR] unknown option $arg."
            echo ""
            help
            exit 2
        fi
    fi
done

if [ "$version_flag" -eq 1 ]; then
    echo "[ERROR] you need to enter a value after -version"
    exit 2
fi

if [ "$timeout_startup_flag" -eq 1 ]; then
    echo "[ERROR] you need to enter a value after -timeout-startup"
    exit 2
fi

if [ "$timeout_shutdown_flag" -eq 1 ]; then
    echo "[ERROR] you need to enter a value after -timeout-shutdown"
    exit 2
fi

if [ "$timeout_test_flag" -eq 1 ]; then
    echo "[ERROR] you need to enter a value after -timeout-test"
    exit 2
fi

if [ "$confluence_logfile_flag" -eq 1 ]; then
    echo "[ERROR] you need to enter a value after -timeout-test"
    exit 2
fi

echo "[INFO] cleaning up"
echo "$ atlas-clean $flags -B" >>"$confluence_logfile"
atlas-clean $flags -B >>"$confluence_logfile" 2>&1

# Reason for </dev/zero: https://community.atlassian.com/t5/Answers-Developer-Questions/How-to-start-a-standalone-JIRA-test-instance-from-Atlassian-SDK/qaq-p/469326
echo "[INFO] starting confluence"
echo "\$ atlas-run -B --server 127.0.0.1 $flags" >>"$confluence_logfile"
cat /dev/zero > "$confluence_input_pipe" &
conflunece_input_pid=$!
#atlas-run -B --server 127.0.0.1 $flags <"$confluence_input_pipe" >>"$confluence_logfile" 2>&1 & 
atlas-debug -B --server 127.0.0.1 $flags <"$confluence_input_pipe" >>"$confluence_logfile" 2>&1 & 
confluence_pid=$!
launchtime=$(timestamp)

# Wait till confluence is up.
confluence_ready=0
echo -n "[INFO] waiting for confluence to start "
while [ $(expr "$(timestamp)" - "$launchtime" ) -lt "$timeout_startup" ]; do
    if ! kill -0 $confluence_pid >/dev/null 2>&1; then
        echo ""
        echo "[ERROR] failed to start confluence (process ended)"
        exit 1
    fi

    if curl -s -f -m 10 "http://127.0.0.1:1990/confluence"; then
        confluence_ready=1
        echo -n " ready"
        break
    fi

    echo -n "."
    sleep 10
done
echo ""

if [ "$confluence_ready" -eq 0 ]; then
    echo "[ERROR] failed to start confluence (timed out)"
    exit 4
fi

run_integration_test "testProcessImaps"
run_integration_test "testProcessPop3"
exit $integration_test_status
