package ievents; // stands for IntrinsicEvents

import market.Price;

/**
 * Created by author.
 * The class receives prices and returns +1 or -1 in case of Directional-
 * Change (DC) Intrinsic Event (IE) upward or downward and also +2 or -2 in case of
 * an Overshoot (OS) IE upward or downward. Otherwise, returns 0;
 *
 * The class offers two different versions of a price move calculation: absolute and relative ones. The first
 * definition may be useful, for example, in case of when we have prices generated by an arithmetical brownian motion.
 * The second, when all prices are similar to ones generated by a geometrical brownian motion.
 */

public class DcOS {

    private long extreme;
    private long prevExtreme;
    private double thresholdUp;  // 1% == 0.01
    private double thresholdDown;
    private double osSizeUp;
    private double osSizeDown;
    private int mode; // +1 for expected upward DC, -1 for expected downward DC
    private boolean initialized;
    private long reference; // is the price at which an OS intrinsic event is observed
    private long latestDCprice; // this is the price of the latest registered DC IE
    private long prevDCprice; // this is the price of the DC IE before the latest one
    private boolean relativeMoves; // shows if the algorithm should compute relative of absolute price changes
    private double osL; // is length of the previous overshoot
    private long tPrevOS, tPrevDcIE, tOS, tDcIE, tExtreme, tOsIE; // times of the tipping points of the intrinsic time

    public DcOS(double thresholdUp, double thresholdDown, int initialMode, double osSizeUp, double osSizeDown, boolean relativeMoves){
        this.initialized = false;
        this.thresholdUp = thresholdUp;
        this.thresholdDown = thresholdDown;
        this.mode = initialMode;
        this.osSizeUp = osSizeUp;
        this.osSizeDown = osSizeDown;
        this.relativeMoves = relativeMoves;
    }

    public DcOS(double thresholdUp, double thresholdDown, int initialMode, double osSizeUp, double osSizeDown, Price initPrice, boolean relativeMoves){
        this.initialized = true;
        this.thresholdUp = thresholdUp;
        this.thresholdDown = thresholdDown;
        this.mode = initialMode;
        this.osSizeUp = osSizeUp;
        this.osSizeDown = osSizeDown;
        extreme = prevExtreme = reference = prevDCprice = latestDCprice = (mode == 1 ? initPrice.getAsk() : initPrice.getBid());
        tPrevOS = tPrevDcIE = tOS = tDcIE = tExtreme = tOsIE = initPrice.getTime();

    }

    public int run(Price aPrice){
        if (relativeMoves){
            return runRelative(aPrice);
        } else {
            return runAbsolute(aPrice);
        }
    }

    /**
     * Uses log function in order to compute a price move
     * @param aPrice is a new price
     * @return +1 or -1 in case of Directional-Change (DC) Intrinsic Event (IE) upward or downward and also +2 or -2
     * in case of an Overshoot (OS) IE upward or downward. Otherwise, returns 0;
     */
    private int runRelative(Price aPrice){
        if (!initialized){
            initialized = true;
            extreme = prevExtreme = reference = prevDCprice = latestDCprice =(mode == 1 ? aPrice.getAsk() : aPrice.getBid());
            tPrevOS = tPrevDcIE = tOS = tDcIE = tExtreme = tOsIE = aPrice.getTime();

        } else {
            if (mode == 1){
                if (aPrice.getAsk() < extreme){
                    extreme = aPrice.getAsk();
                    tExtreme = aPrice.getTime();
                    if ( -Math.log((double) extreme / reference) >= osSizeDown){
                        reference = extreme;
                        tOsIE = aPrice.getTime();
                        return -2;
                    }
                    return 0;
                } else if (Math.log((double) aPrice.getBid() / extreme) >= thresholdUp){
                    osL = -Math.log((double) extreme / latestDCprice);
                    tPrevOS = tOS;
                    tPrevDcIE = tDcIE;
                    tOS = tExtreme;
                    tDcIE = aPrice.getTime();
                    tExtreme = aPrice.getTime();
                    prevDCprice = latestDCprice;
                    latestDCprice = aPrice.getBid();
                    prevExtreme = extreme;
                    extreme = reference = aPrice.getBid();
                    mode *= -1;
                    return 1;
                }
            }
            else if (mode == -1){
                if (aPrice.getBid() > extreme){
                    extreme = aPrice.getBid();
                    tExtreme = aPrice.getTime();
                    if (Math.log((double) extreme / reference) >= osSizeUp){
                        reference = extreme;
                        tOsIE = aPrice.getTime();
                        return 2;
                    }
                    return 0;
                } else if (-Math.log((double) aPrice.getAsk() / extreme) >= thresholdDown){
                    osL = Math.log((double) extreme / latestDCprice);
                    tPrevOS = tOS;
                    tPrevDcIE = tDcIE;
                    tOS = tExtreme;
                    tDcIE = aPrice.getTime();
                    tExtreme = aPrice.getTime();
                    prevDCprice = latestDCprice;
                    latestDCprice = aPrice.getAsk();
                    prevExtreme = extreme;
                    extreme = reference = aPrice.getAsk();
                    mode *= -1;
                    return -1;
                }
            }
        }

        return 0;
    }

    /**
     * Uses absolute values a price move
     * @param aPrice is a new price
     * @return +1 or -1 in case of Directional-Change (DC) Intrinsic Event (IE) upward or downward and also +2 or -2
     * in case of an Overshoot (OS) IE upward or downward. Otherwise, returns 0;
     */
    private int runAbsolute(Price aPrice){
        if (!initialized){
            initialized = true;
            extreme = prevExtreme = reference = prevDCprice = latestDCprice =(mode == 1 ? aPrice.getAsk() : aPrice.getBid());
        } else {
            if (mode == 1){
                if (aPrice.getAsk() < extreme){
                    extreme = aPrice.getAsk();
                    tExtreme = aPrice.getTime();
                    if ( -(extreme - reference) >= osSizeDown){
                        reference = extreme;
                        tOsIE = aPrice.getTime();
                        return -2;
                    }
                    return 0;
                } else if (aPrice.getBid() - extreme >= thresholdUp){
                    osL = -(extreme - latestDCprice);
                    tPrevOS = tOS;
                    tPrevDcIE = tDcIE;
                    tOS = tExtreme;
                    tDcIE = aPrice.getTime();
                    tExtreme = aPrice.getTime();
                    prevDCprice = latestDCprice;
                    latestDCprice = aPrice.getBid();
                    prevExtreme = extreme;
                    extreme = reference = aPrice.getBid();
                    mode *= -1;
                    return 1;
                }
            }
            else if (mode == -1){
                if (aPrice.getBid() > extreme){
                    extreme = aPrice.getBid();
                    tExtreme = aPrice.getTime();
                    if (extreme - reference >= osSizeUp){
                        reference = extreme;
                        tOsIE = aPrice.getTime();
                        return 2;
                    }
                    return 0;
                } else if (-(aPrice.getAsk() - extreme) >= thresholdDown){
                    osL = (extreme - latestDCprice);
                    tPrevOS = tOS;
                    tPrevDcIE = tDcIE;
                    tOS = tExtreme;
                    tDcIE = aPrice.getTime();
                    tExtreme = aPrice.getTime();
                    prevDCprice = latestDCprice;
                    latestDCprice = aPrice.getAsk();
                    prevExtreme = extreme;
                    extreme = reference = aPrice.getAsk();
                    mode *= -1;
                    return -1;
                }
            }
        }

        return 0;
    }

    /**
     * The function computes OS deviation defined as a squared difference between the size of overshoot and
     * correspondent threshold. Details can be found in "Bridging the gap between physical and intrinsic time"
     * @return double variability of an overshoot.
     */
    public double computeSqrtOsDeviation(){
        double sqrtOsDeviation;
        if (mode == 1){
            sqrtOsDeviation = Math.pow(osL - thresholdUp, 2);
        } else {
            sqrtOsDeviation = Math.pow(osL - thresholdDown, 2);
        }
        return sqrtOsDeviation;
    }

    public double getOsL(){
        return osL;
    }

    public long getLatestDCprice() {
        return latestDCprice;
    }

    public long getPrevDCprice() {
        return prevDCprice;
    }

    public long getExtreme() {
        return extreme;
    }

    public long getPrevExtreme() { return prevExtreme; }

    public void setExtreme(long extreme) {
        this.extreme = extreme;
    }

    public double getThresholdUp() {
        return thresholdUp;
    }

    public void setThresholdUp(float thresholdUp) {
        this.thresholdUp = thresholdUp;
    }

    public double getThresholdDown() {
        return thresholdDown;
    }

    public void setThresholdDown(float thresholdDown) {
        this.thresholdDown = thresholdDown;
    }

    public double getOsSizeUp() {
        return osSizeUp;
    }

    public void setOsSizeUp(float osSizeUp) {
        this.osSizeUp = osSizeUp;
    }

    public double getOsSizeDown() {
        return osSizeDown;
    }

    public void setOsSizeDown(float osSizeDown) {
        this.osSizeDown = osSizeDown;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public long getReference() {
        return reference;
    }

    public void setReference(long reference) {
        this.reference = reference;
    }

    public long gettPrevOS() {
        return tPrevOS;
    }

    public long gettPrevDcIE() {
        return tPrevDcIE;
    }

    public long gettOS() {
        return tOS;
    }

    public long gettDcIE() {
        return tDcIE;
    }

    public long gettExtreme() {
        return tExtreme;
    }

    public long gettOsIE() {
        return tOsIE;
    }
}