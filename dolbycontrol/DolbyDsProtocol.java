package com.mdph.dolbycontrol;

final class DolbyDsProtocol {
    static final String BACKEND_PACKAGE = "com.cirrest.atmos.DAX2";
    static final String BACKEND_SERVICE = "com.atmos.DsService";
    static final String INTERFACE_TOKEN = "android.dolby.IDs";

    static final int TRANSACTION_SET_DS_ON = 1;
    static final int TRANSACTION_GET_DS_ON = 2;
    static final int TRANSACTION_GET_PROFILE_COUNT = 4;
    static final int TRANSACTION_SET_SELECTED_PROFILE = 8;
    static final int TRANSACTION_GET_SELECTED_PROFILE = 9;
    static final int TRANSACTION_SET_PROFILE_SETTINGS = 10;
    static final int TRANSACTION_GET_PROFILE_SETTINGS = 11;
    static final int TRANSACTION_SET_IEQ_PRESET = 17;
    static final int TRANSACTION_GET_IEQ_PRESET = 18;
    static final int TRANSACTION_SET_GEQ = 20;
    static final int TRANSACTION_GET_GEQ = 21;
    static final int TRANSACTION_SET_DS_AP_PARAM = 22;

    static final String PARAM_DIALOG_AMOUNT = "dea";
    static final String PARAM_LEVELER_AMOUNT = "dvla";

    private DolbyDsProtocol() {
    }

    static float[] toServiceGeq(int[] dbValues) {
        if (dbValues == null) {
            return new float[0];
        }
        float[] gains = new float[dbValues.length];
        for (int i = 0; i < dbValues.length; i++) {
            gains[i] = GeqGainMapper.sanitizeDb(dbValues[i]);
        }
        return gains;
    }

    static int[] fromServiceGeq(float[] gains) {
        if (gains == null) {
            return new int[0];
        }
        int[] dbValues = new int[gains.length];
        for (int i = 0; i < gains.length; i++) {
            dbValues[i] = GeqGainMapper.sanitizeDb(Math.round(gains[i]));
        }
        return dbValues;
    }
}
