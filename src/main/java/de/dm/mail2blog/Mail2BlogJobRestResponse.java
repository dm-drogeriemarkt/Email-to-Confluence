package de.dm.mail2blog;

import com.atlassian.scheduler.JobRunnerResponse;
import lombok.*;

import javax.xml.bind.annotation.*;
@XmlRootElement(name = "result")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Mail2BlogJobRestResponse {
    String status = "";
    String message = "";
}
