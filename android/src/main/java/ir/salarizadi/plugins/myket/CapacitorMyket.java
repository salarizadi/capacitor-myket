package ir.salarizadi.plugins.myket;

import com.getcapacitor.Logger;

public class CapacitorMyket {

    public String echo(String value) {
        Logger.info("Echo", value);
        return value;
    }
}
