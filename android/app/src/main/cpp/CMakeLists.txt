cmake_minimum_required(VERSION 3.22.1)

project("lecturecap_native")

set(WHISPER_ROOT "${CMAKE_SOURCE_DIR}/../../../../third_party/whisper.cpp")

set(GGML_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(GGML_BUILD_SERVER OFF CACHE BOOL "" FORCE)
add_subdirectory("${WHISPER_ROOT}/ggml" "${CMAKE_BINARY_DIR}/ggml")

add_library(lecturecap_native SHARED whisper_jni.cpp "${WHISPER_ROOT}/src/whisper.cpp")

target_compile_definitions(lecturecap_native PRIVATE
	GGML_USE_CPU
	WHISPER_VERSION="1.9.3-dev")
target_include_directories(lecturecap_native PRIVATE
	"${WHISPER_ROOT}/include"
	"${WHISPER_ROOT}/src"
	"${WHISPER_ROOT}/ggml/include"
	"${WHISPER_ROOT}/ggml/src"
	"${WHISPER_ROOT}/ggml/src/ggml-cpu")

find_library(log-lib log)

target_link_libraries(lecturecap_native ${log-lib} android ggml)
