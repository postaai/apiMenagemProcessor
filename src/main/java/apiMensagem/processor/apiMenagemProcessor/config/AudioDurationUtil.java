package apiMensagem.processor.apiMenagemProcessor.config;

public final class AudioDurationUtil {

    private static final int WHATSAPP_OPUS_BITRATE = 24_000; // bps

    private AudioDurationUtil() {}

    public static int estimateDurationSeconds(byte[] audioBytes) {
        if (audioBytes == null || audioBytes.length == 0) {
            return Integer.MAX_VALUE;
        }

        long bits = (long) audioBytes.length * 8;
        return (int) Math.ceil((double) bits / WHATSAPP_OPUS_BITRATE);
    }
}
