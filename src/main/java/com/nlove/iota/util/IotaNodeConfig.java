package com.nlove.iota.util;

public class IotaNodeConfig {

    private String apiHttpsHostname;
    private String zmqUrl;

    public String getZmqUrl() {
        return zmqUrl;
    }

    public void setZmqUrl(String zmqUrl) {
        this.zmqUrl = zmqUrl;
    }

    public String getApiHttpsHostname() {
        return apiHttpsHostname;
    }

    public void setApiHttpsHostname(String apiHttpsHostname) {
        this.apiHttpsHostname = apiHttpsHostname;
    }
}
