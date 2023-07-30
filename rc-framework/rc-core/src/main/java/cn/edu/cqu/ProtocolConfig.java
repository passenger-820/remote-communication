package cn.edu.cqu;

import lombok.Getter;

@Getter
public class ProtocolConfig {

    private String protocolType;

    public ProtocolConfig(String protocolType) {
        this.protocolType = protocolType;
    }
}
