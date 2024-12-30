package com.macaron.mqcsdk;

public  class VoiceDetect {
    /**
     * 计算输入数据段的db值，按公式应该为20*Math.log10(当前振幅值/最大振幅值)；
     * 位深为16bit，则代表两个字节表示一个音量采集单位；
     * 此处用平方和平均值进行计算；
     *
     * @param data 输入pcm音频数据
     * @param bit  位深，8或16
     * @return 当前分贝值
     */
    public static int calculateVolume(byte[] data, int bit) {
        int[] newBuffer = null;
        int len = data.length;
        int index;

        //排列
        if (bit == 8) {
            newBuffer = new int[len];
            for (index = 0; index < len; ++index) {
                newBuffer[index] = data[index];
            }
        }
        //平方和求平均值
        if (newBuffer != null && newBuffer.length != 0) {
            float avg = 0.0F;
            for (int i = 0; i < newBuffer.length; ++i) {
                avg += (float) (newBuffer[i] * newBuffer[i]);
            }
            avg /= (float) newBuffer.length;
            return (int) (10.0D * Math.log10(avg + 1));
        } else {
            return 0;
        }
    }

}
