package io.dcloud.feature.speech.dialog;

import io.dcloud.RInformation;
import io.dcloud.common.DHInterface.IReflectAble;

public interface SpeechR extends IReflectAble {
    public static final int LAYOUT_DIALOG = RInformation.getInt("layout","speech_dialog");
    public static final int ID_VOICE_2_TEXT_LAYOUT = RInformation.getInt("id","voiceToTextAnimationLayout");
    public static final int ID_VOICE_TITLE = RInformation.getInt("id","voiceDialogTitle");
    public static final int ID_VOICE_VOLUME = RInformation.getInt("id","voiceToTextAnimationIV");
    public static final int ID_RECOG_ANIM = RInformation.getInt("id","voiceDialogAnimation");
}
