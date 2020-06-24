# Sets the minimum version of CMake required to build the native
# library.
cmake_minimum_required(VERSION 3.4.1)

# Creates the project's shared lib: libqdicteng.so.
# The lib is loaded by this project's Java code in MainActivity.java:
#     System.loadLibrary("qdicteng");
# The lib name in both places must match.
add_library(qdicteng SHARED
            QDictEng.c
            DictEng.c
            DictZipLib.c
            DictIndexUtil.c
            OffsetIndex.c
            IdxsynFile.c
            InfoFile.c
            plugs/DictPlugs.c
            plugs/Powerword.c
            utils/MapFile.c
            utils/DictUtils.c
            utils/Distance.c
            utils/QSort.c
            utils/Pattern.c
            )

target_include_directories(qdicteng PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/utils
        ${CMAKE_CURRENT_SOURCE_DIR}/plugs
        )

set(ANDROID_STL c++_shared)

# Include libraries needed for qdicteng lib
target_link_libraries(qdicteng
                      android
                      z
                      log)
