package me.donlis.vbanner;

public final class BannerUtil {

    public static int getRealPosition(boolean isCanLoop, int position, int pageSize) {
        if (pageSize == 0) return 0;
        return isCanLoop ? (position - 1 + pageSize) % pageSize : (position + pageSize) % pageSize;
    }

}
