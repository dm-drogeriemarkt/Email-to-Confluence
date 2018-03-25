package de.dm.mail2blog;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

@XmlRootElement(name = "result")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Mail2BlogJobRestResponse implements Serializable {
    String status = "";
    String message = "";
}
