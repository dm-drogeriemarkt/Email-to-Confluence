package it.de.dm.mail2blog;

import lombok.Data;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement
@Data
public class IntegrationTestResponse implements Serializable {
    @XmlElement
    boolean successful = true;

    @XmlElement
    String message = "";

    @XmlElement
    String stacktrace = "";
}
