#include <jni.h>
#include <android/log.h>
#include <mutex>
#include <sstream>
#include <string>

#include "whisper.h"

namespace {
constexpr char TAG[] = "WhisperJNI";
std::mutex context_mutex;
whisper_context * context = nullptr;

std::string escape_json(const char * text) {
    std::string escaped;
    if (text == nullptr) return escaped;
    for (const unsigned char * cursor = reinterpret_cast<const unsigned char *>(text); *cursor; ++cursor) {
        switch (*cursor) {
            case '"': escaped += "\\\""; break;
            case '\\': escaped += "\\\\"; break;
            case '\n': escaped += "\\n"; break;
            case '\r': escaped += "\\r"; break;
            case '\t': escaped += "\\t"; break;
            default:
                if (*cursor < 0x20) {
                    escaped += ' ';
                } else {
                    escaped += static_cast<char>(*cursor);
                }
        }
    }
    return escaped;
}
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_lecturecap_stt_NativeWhisperEngine_nativeInitModel(JNIEnv * env, jclass, jstring model_path) {
    const char * path = env->GetStringUTFChars(model_path, nullptr);
    std::lock_guard<std::mutex> lock(context_mutex);
    if (context != nullptr) {
        whisper_free(context);
        context = nullptr;
    }
    whisper_context_params params = whisper_context_default_params();
    params.use_gpu = false;
    context = whisper_init_from_file_with_params(path, params);
    env->ReleaseStringUTFChars(model_path, path);
    __android_log_print(context ? ANDROID_LOG_INFO : ANDROID_LOG_ERROR, TAG,
                        "WHISPER_MODEL_LOADED=%s", context ? "true" : "false");
    return context != nullptr ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_lecturecap_stt_NativeWhisperEngine_nativeReleaseModel(JNIEnv *, jclass) {
    std::lock_guard<std::mutex> lock(context_mutex);
    if (context != nullptr) {
        whisper_free(context);
        context = nullptr;
    }
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_lecturecap_stt_NativeWhisperEngine_nativeTranscribe(JNIEnv * env, jclass, jfloatArray audio) {
    std::lock_guard<std::mutex> lock(context_mutex);
    if (context == nullptr || audio == nullptr) return nullptr;

    const jsize sample_count = env->GetArrayLength(audio);
    jfloat * samples = env->GetFloatArrayElements(audio, nullptr);
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;
    params.translate = false;
    params.language = "en";
    params.n_threads = 4;
    params.no_context = true;
    params.single_segment = false;

    __android_log_print(ANDROID_LOG_INFO, TAG, "WHISPER_INFERENCE_STARTED samples=%d", sample_count);
    const int result = whisper_full(context, params, samples, sample_count);
    env->ReleaseFloatArrayElements(audio, samples, JNI_ABORT);
    if (result != 0) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "WHISPER_INFERENCE_COMPLETED success=false");
        return nullptr;
    }

    std::ostringstream json;
    json << '[';
    for (int index = 0; index < whisper_full_n_segments(context); ++index) {
        if (index > 0) json << ',';
        json << "{\"id\":" << (index + 1)
             << ",\"start\":" << (whisper_full_get_segment_t0(context, index) / 100.0)
             << ",\"end\":" << (whisper_full_get_segment_t1(context, index) / 100.0)
             << ",\"text\":\"" << escape_json(whisper_full_get_segment_text(context, index)) << "\"}";
    }
    json << ']';
    __android_log_print(ANDROID_LOG_INFO, TAG, "WHISPER_INFERENCE_COMPLETED success=true segments=%d",
                        whisper_full_n_segments(context));
    return env->NewStringUTF(json.str().c_str());
}