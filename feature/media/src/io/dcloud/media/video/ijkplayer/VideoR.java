package io.dcloud.media.video.ijkplayer;

import io.dcloud.RInformation;
import io.dcloud.common.DHInterface.IReflectAble;


public interface VideoR  extends IReflectAble {

    public static int VIDEO_IJK_LAYOUT_PLAYER_VIEW = RInformation.getInt("layout", "video_layout_player_view");


    public static int VIDEO_IJK_ID_VIDEO_VIEW = RInformation.getInt("id", "video_view");
    public static int VIDEO_IJK_ID_IV_THUMB = RInformation.getInt("id", "iv_thumb");
    public static int VIDEO_IJK_ID_PD_LOADING = RInformation.getInt("id", "pb_loading");
    public static int VIDEO_IJK_ID_TV_VOLUME = RInformation.getInt("id", "tv_volume");
    public static int VIDEO_IJK_ID_TV_BRIGHTNESS= RInformation.getInt("id", "tv_brightness");
    public static int VIDEO_IJK_ID_TV_FAST_FORWARD = RInformation.getInt("id", "tv_fast_forward");
    public static int VIDEO_IJK_ID_FL_TOUCH_LAYOUT = RInformation.getInt("id", "fl_touch_layout");
//    public static int VIDEO_IJK_ID_IV_BACK = RInformation.getInt("id", "iv_back");
//    public static int VIDEO_IJK_ID_TV_TITLE = RInformation.getInt("id", "tv_title");
//    public static int VIDEO_IJK_ID_FULLSCREEN_TOP_BAR = RInformation.getInt("id", "fullscreen_top_bar");
    public static int VIDEO_IJK_ID_IV_PLAY = RInformation.getInt("id", "iv_play");
    public static int VIDEO_IJK_ID_TV_CUR_TIME = RInformation.getInt("id", "tv_cur_time");
    public static int VIDEO_IJK_ID_PLAYER_SEEK = RInformation.getInt("id", "player_seek");
    public static int VIDEO_IJK_ID_TV_END_TIME = RInformation.getInt("id", "tv_end_time");
    public static int VIDEO_IJK_ID_IV_FULLSCREEN = RInformation.getInt("id", "iv_fullscreen");
    public static int VIDEO_IJK_ID_LL_BOTTOM_BAR = RInformation.getInt("id", "ll_bottom_bar");
    public static int VIDEO_IJK_ID_FL_VIDEO_BOX = RInformation.getInt("id", "fl_video_box");
    public static int VIDEO_IJK_ID_IV_PLAY_CIRCLE = RInformation.getInt("id", "iv_play_circle");
    public static int VIDEO_IJK_ID_TV_RECOVER_SCREEN = RInformation.getInt("id", "tv_recover_screen");
    public static int VIDEO_IJK_ID_TV_RELOAD = RInformation.getInt("id", "tv_reload");
    public static int VIDEO_IJK_ID_FL_RELOAD_LAYOUT = RInformation.getInt("id", "fl_reload_layout");

    public static int VIDEO_IJK_ID_IV_DANMAKU_CONTROL = RInformation.getInt("id", "iv_danmaku_control");
    public static int VIDEO_IJK_ID_SV_DANMAKU = RInformation.getInt("id", "sv_danmaku");
//    public static int VIDEO_IJK_ID_DP_BATTERY = RInformation.getInt("id", "pb_battery");
//    public static int VIDEO_IJK_ID_TV_SYSTEM_TIME = RInformation.getInt("id", "tv_system_time");



    public static int VIDEO_IJK_DIMEN_ASPECT_BNT_SIZE= RInformation.getInt("dimen", "video_aspect_btn_size");
    public static int VIDEO_IJK_DIMEN_DANMAKU_INPUT_BTN_SIZE= RInformation.getInt("dimen", "video_danmaku_input_options_color_radio_btn_size");


    public static int VIDEO_IJK_DRAWABLE_IC_BATTERY_CHARGING = RInformation.getInt("drawable", "video_ic_battery_charging");
    public static int VIDEO_IJK_DRAWABLE_IC_BATTERY_RED = RInformation.getInt("drawable", "video_ic_battery_red");
    public static int VIDEO_IJK_DRAWABLE_IC_BATTERY = RInformation.getInt("drawable", "video_ic_battery");


}
