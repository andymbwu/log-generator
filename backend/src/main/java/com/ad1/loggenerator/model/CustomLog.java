package com.ad1.loggenerator.model;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomLog {
    
    // the frequency to include the custom log
    @NotNull
    private Double frequency;
    // the fields in the custom log
    @NotEmpty
    private Map<String, Object> fields;

    @Override
    public boolean equals(Object object) {

        if (object == null) {
            return false;
        }

        if (!(object instanceof CustomLog)) {
            return false;
        }

        CustomLog customLog = (CustomLog)object;
        
        if (!frequency.equals(customLog.getFrequency())) {
            return false;
        }

        if (!customLog.getFields().keySet().equals(fields.keySet())) {
            return false;
        }

        return true;
    }
}
