cmake_minimum_required(VERSION 3.22.1)
project(llama_android LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(LLAMA_BUILD_COMMON OFF CACHE BOOL "common" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "tests" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "examples" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "server" FORCE)

add_library(llama_jni SHARED
    llama-bridge.cpp
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(llama_jni
    ${log-lib}
    ${android-lib}
)
