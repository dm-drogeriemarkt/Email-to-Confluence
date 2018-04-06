package it.de.dm.mail2blog;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement(name = "testresult")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class IntegrationTestRestRunnerModel implements Serializable {
    @XmlElement
    boolean successful = true;

    @XmlElement
    String message = "";

    @XmlElement
    String stacktrace = "";
}
