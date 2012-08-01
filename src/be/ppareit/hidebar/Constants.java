package be.ppareit.hidebar;

public class Constants {

    /** Is the application build as demo or for paid users */
    enum MarketType {
        DEMO, PAID;
    }

    static final MarketType MARKETTYPE = MarketType.DEMO;

    static final String ACTION_BARHIDDEN = "be.ppareit.hidebar.ACTION_BARHIDDEN";
    static final String ACTION_BARSHOWN = "be.ppareit.hidebar.ACTION_BARSHOWN";
}
