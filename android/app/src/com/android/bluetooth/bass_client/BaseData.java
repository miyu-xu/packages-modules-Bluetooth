/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.bluetooth.bass_client;

import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

/**
 * Helper class to parse the Broadcast Announcement BASE data
 */
class BaseData {
    private static final ThreadLocal<Integer> sParseOffset =
            new ThreadLocal<Integer>() {
                @Override protected Integer initialValue() {
                    return 0;
                }
            };
    private static final String TAG = "Bassclient-BaseData";
    private static final byte UNKNOWN_CODEC = (byte) 0xFE;
    private static final int METADATA_LEVEL1 = 1;
    private static final int METADATA_LEVEL2 = 2;
    private static final int METADATA_LEVEL3 = 3;
    private static final int METADATA_PRESENTATIONDELAY_LENGTH = 3;
    private static final int METADATA_CODEC_LENGTH = 5;
    private static final int METADATA_UNKNOWN_CODEC_LENGTH = 1;
    private static final int CODEC_CAPABILITIES_SAMPLE_RATE_TYPE = 1;
    private static final int CODEC_CAPABILITIES_FRAME_DURATION_TYPE = 2;
    private static final int CODEC_CAPABILITIES_CHANNEL_COUNT_TYPE = 3;
    private static final int CODEC_CAPABILITIES_OCTETS_PER_FRAME_TYPE = 4;
    private static final int CODEC_CAPABILITIES_MAX_FRAMES_PER_SDU_TYPE = 5;
    private static final int CODEC_CONFIGURATION_SAMPLE_RATE_TYPE = 0x01;
    private static final int CODEC_CONFIGURATION_FRAME_DURATION_TYPE = 0x02;
    private static final int CODEC_CONFIGURATION_CHANNEL_ALLOCATION_TYPE = 0x03;
    private static final int CODEC_CONFIGURATION_OCTETS_PER_FRAME_TYPE = 0x04;
    private static final int CODEC_CONFIGURATION_BLOCKS_PER_SDU_TYPE = 0x05;
    private static final int METADATA_PREFERRED_CONTEXTS_TYPE = 0x01;
    private static final int METADATA_STREAMING_CONTEXTS_TYPE = 0x02;
    private static final int METADATA_PROGRAM_INFO_TYPE = 0x03;
    private static final int METADATA_LANGUAGE_TYPE = 0x04;
    private static final int METADATA_CCID_LIST_TYPE = 0x05;
    private static final int METADATA_PARENTAL_RATING_TYPE = 0x06;
    private static final int METADATA_PROGRAM_INFO_URI_TYPE = 0x07;
    private static final int METADATA_EXTENDED_TYPE = 0xFE;
    private static final int METADATA_VENDOR_TYPE = 0xFF;
    private static final int CODEC_AUDIO_LOCATION_FRONT_LEFT = 0x01000000;
    private static final int CODEC_AUDIO_LOCATION_FRONT_RIGHT = 0x02000000;
    private static final int CODEC_AUDIO_SAMPLE_RATE_8K = 0x01;
    private static final int CODEC_AUDIO_SAMPLE_RATE_16K = 0x03;
    private static final int CODEC_AUDIO_SAMPLE_RATE_24K = 0x05;
    private static final int CODEC_AUDIO_SAMPLE_RATE_32K = 0x06;
    private static final int CODEC_AUDIO_SAMPLE_RATE_44P1K = 0x07;
    private static final int CODEC_AUDIO_SAMPLE_RATE_48K = 0x08;
    private static final int CODEC_AUDIO_FRAME_DURATION_7P5MS = 0x00;
    private static final int CODEC_AUDIO_FRAME_DURATION_10MS = 0x01;

    private final BaseInformation mLevelOne;
    private final ArrayList<BaseInformation> mLevelTwo;
    private final ArrayList<BaseInformation> mLevelThree;
    private int mNumBISIndices = 0;

    public static class BaseInformation {
        public byte[] presentationDelay = new byte[3];
        public byte[] codecId = new byte[5];
        public byte codecConfigLength;
        public byte[] codecConfigInfo;
        public byte metaDataLength;
        public byte[] metaData;
        public byte numSubGroups;
        public byte[] bisIndices;
        public byte index;
        public int subGroupId;
        public int level;
        public LinkedHashSet<String> keyCodecCfgDiff;
        public LinkedHashSet<String> keyMetadataDiff;
        public String diffText;
        public String description;
        public byte[] consolidatedCodecId;
        public Set<String> consolidatedMetadata;
        public Set<String> consolidatedCodecInfo;
        public HashMap<Integer, String> consolidatedUniqueCodecInfo;
        public HashMap<Integer, String> consolidatedUniqueMetadata;

        BaseInformation() {
            presentationDelay = new byte[3];
            codecId = new byte[5];
            codecConfigLength = 0;
            codecConfigInfo = null;
            metaDataLength = 0;
            metaData = null;
            numSubGroups = 0;
            bisIndices = null;
            index = (byte) 0xFF;
            level = 0;
            keyCodecCfgDiff = new LinkedHashSet<String>();
            keyMetadataDiff = new LinkedHashSet<String>();
            consolidatedMetadata = new LinkedHashSet<String>();
            consolidatedCodecInfo = new LinkedHashSet<String>();
            consolidatedCodecId = new byte[5];
            consolidatedUniqueMetadata = new HashMap<Integer, String>();
            consolidatedUniqueCodecInfo = new HashMap<Integer, String>();
            diffText = new String("");
            description = new String("");
            log("BaseInformation is Initialized");
        }

        boolean isCodecIdUnknown() {
            return (codecId != null && codecId[4] == (byte) UNKNOWN_CODEC);
        }

        void print() {
            log("**BEGIN: Base Information**");
            log("**Level: " + level + "***");
            if (level == 1) {
                log("presentationDelay: " + Arrays.toString(presentationDelay));
            }
            if (level == 2) {
                log("codecId: " + Arrays.toString(codecId));
            }
            if (level == 2 || level == 3) {
                log("codecConfigLength: " + codecConfigLength);
                log("subGroupId: " + subGroupId);
            }
            if (codecConfigLength != (byte) 0) {
                log("codecConfigInfo: " + Arrays.toString(codecConfigInfo));
            }
            if (level == 2) {
                log("metaDataLength: " + metaDataLength);
                if (metaDataLength != (byte) 0) {
                    log("metaData: " + Arrays.toString(metaData));
                }
                if (level == 1 || level == 2) {
                    log("numSubGroups: " + numSubGroups);
                }
            }
            if (level == 2) {
                log("Level2: Key Metadata differentiators");
                if (keyMetadataDiff != null) {
                    Iterator<String> itr = keyMetadataDiff.iterator();
                    for (int k = 0; itr.hasNext(); k++) {
                        log("keyMetadataDiff:[" + k + "]:"
                                + Arrays.toString(itr.next().getBytes()));
                    }
                }
                log("END: Level2: Key Metadata differentiators");
                log("Level2: Key CodecConfig differentiators");
                if (keyCodecCfgDiff != null) {
                    Iterator<String> itr = keyCodecCfgDiff.iterator();
                    for (int k = 0; itr.hasNext(); k++) {
                        log("LEVEL2: keyCodecCfgDiff:[" + k + "]:"
                                + Arrays.toString(itr.next().getBytes()));
                    }
                }
                log("END: Level2: Key CodecConfig differentiators");
                log("LEVEL2: diffText: " + diffText);
            }
            if (level == 3) {
                log("Level3: Key CodecConfig differentiators");
                if (keyCodecCfgDiff != null) {
                    Iterator<String> itr = keyCodecCfgDiff.iterator();
                    for (int k = 0; itr.hasNext(); k++) {
                        log("LEVEL3: keyCodecCfgDiff:[" + k + "]:"
                                + Arrays.toString(itr.next().getBytes()));
                    }
                }
                log("END: Level3: Key CodecConfig differentiators");
                log("index: " + index);
                log("LEVEL3: diffText: " + diffText);
            }
            log("**END: Base Information****");
        }
    }

    BaseData(BaseInformation levelOne, ArrayList<BaseInformation> levelTwo,
             ArrayList<BaseInformation> levelThree, int numOfBISIndices) {
        mLevelOne = levelOne;
        mLevelTwo = levelTwo;
        mLevelThree = levelThree;
        mNumBISIndices = numOfBISIndices;
    }

    private static int getParseOffset() {
        return sParseOffset.get();
    }
    private static void setParseOffset(int offset) {
        sParseOffset.set(offset);
    }
    private static void increaseParseOffset(int added) {
        sParseOffset.set(sParseOffset.get() + added);
    }

    static BaseData parseBaseData(byte[] serviceData) {
        if (serviceData == null) {
            Log.e(TAG, "Invalid service data for BaseData construction");
            throw new IllegalArgumentException("Basedata: serviceData is null");
        }
        BaseInformation levelOne = new BaseInformation();
        ArrayList<BaseInformation> levelTwo = new ArrayList<BaseInformation>();
        ArrayList<BaseInformation> levelThree = new ArrayList<BaseInformation>();
        int numOfBISIndices = 0;
        log("BASE input" + Arrays.toString(serviceData));

        // Parse Level 1 base
        levelOne.level = METADATA_LEVEL1;
        setParseOffset(0);
        System.arraycopy(serviceData, getParseOffset(), levelOne.presentationDelay, 0, 3);
        increaseParseOffset(METADATA_PRESENTATIONDELAY_LENGTH);
        levelOne.numSubGroups = serviceData[getParseOffset()];
        increaseParseOffset(1);
        levelOne.print();
        log("levelOne subgroups" + levelOne.numSubGroups);
        for (int i = 0; i < (int) levelOne.numSubGroups; i++) {
            BaseInformation node2 = parseLevelTwo(serviceData, i);
            if (node2 == null) {
                Log.e(TAG, "Error: parsing Level 2");
                return null;
            }
            numOfBISIndices += node2.numSubGroups;
            levelTwo.add(node2);
            node2.print();
            for (int k = 0; k < node2.numSubGroups; k++) {
                BaseInformation node3 = parseLevelThree(serviceData);
                if (node3 == null) {
                    Log.e(TAG, "Error: parsing Level 3");
                    return null;
                }
                levelThree.add(node3);
                node3.print();
            }
        }
        consolidateBaseofLevelTwo(levelTwo, levelThree);
        return new BaseData(levelOne, levelTwo, levelThree, numOfBISIndices);
    }

    private static BaseInformation parseLevelTwo(byte[] serviceData, int groupIndex) {
        log("Parsing Level 2");
        BaseInformation node = new BaseInformation();
        node.level = METADATA_LEVEL2;
        node.subGroupId = groupIndex;
        node.numSubGroups = serviceData[getParseOffset()];
        increaseParseOffset(1);
        if (serviceData[getParseOffset()] == (byte) UNKNOWN_CODEC) {
            // Place It in the last byte of codecID
            System.arraycopy(serviceData, getParseOffset(), node.codecId,
                    METADATA_CODEC_LENGTH - 1, METADATA_UNKNOWN_CODEC_LENGTH);
            increaseParseOffset(METADATA_UNKNOWN_CODEC_LENGTH);
            log("codecId is FE");
        } else {
            System.arraycopy(serviceData, getParseOffset(), node.codecId,
                    0, METADATA_CODEC_LENGTH);
            increaseParseOffset(METADATA_CODEC_LENGTH);
        }
        node.codecConfigLength = serviceData[getParseOffset()];
        increaseParseOffset(1);
        if (node.codecConfigLength != 0) {
            node.codecConfigInfo = new byte[(int) node.codecConfigLength];
            System.arraycopy(serviceData, getParseOffset(), node.codecConfigInfo,
                    0, (int) node.codecConfigLength);
            increaseParseOffset((int) node.codecConfigLength);
        }
        node.metaDataLength = serviceData[getParseOffset()];
        increaseParseOffset(1);
        if (node.metaDataLength != 0) {
            node.metaData = new byte[(int) node.metaDataLength];
            System.arraycopy(serviceData, getParseOffset(),
                    node.metaData, 0, (int) node.metaDataLength);
            increaseParseOffset((int) node.metaDataLength);
        }
        return node;
    }

    private static BaseInformation parseLevelThree(byte[] serviceData) {
        log("Parsing Level 3");
        BaseInformation node = new BaseInformation();
        node.level = METADATA_LEVEL3;
        node.index = serviceData[getParseOffset()];
        increaseParseOffset(1);
        node.codecConfigLength = serviceData[getParseOffset()];
        increaseParseOffset(1);
        if (node.codecConfigLength != 0) {
            node.codecConfigInfo = new byte[(int) node.codecConfigLength];
            System.arraycopy(serviceData, getParseOffset(),
                    node.codecConfigInfo, 0, (int) node.codecConfigLength);
            increaseParseOffset((int) node.codecConfigLength);
        }
        return node;
    }

    static void consolidateBaseofLevelTwo(ArrayList<BaseInformation> levelTwo,
            ArrayList<BaseInformation> levelThree) {
        int startIdx = 0;
        int children = 0;
        for (int i = 0; i < levelTwo.size(); i++) {
            startIdx = startIdx + children;
            children = children + levelTwo.get(i).numSubGroups;
            consolidateBaseofLevelThree(levelTwo, levelThree,
                    i, startIdx, levelTwo.get(i).numSubGroups);
        }
        // Eliminate Duplicates at Level 3
        for (int i = 0; i < levelThree.size(); i++) {
            Map<Integer, String> uniqueMds = new HashMap<Integer, String>();
            Map<Integer, String> uniqueCcis = new HashMap<Integer, String>();
            Set<String> Csfs = levelThree.get(i).consolidatedCodecInfo;
            if (Csfs.size() > 0) {
                Iterator<String> itr = Csfs.iterator();
                for (int j = 0; itr.hasNext(); j++) {
                    byte[] ltvEntries = itr.next().getBytes();
                    int k = 0;
                    byte length = ltvEntries[k++];
                    byte[] ltv = new byte[length + 1];
                    ltv[0] = length;
                    System.arraycopy(ltvEntries, k, ltv, 1, length);
                    int type = (int) ltv[1];
                    String s = uniqueCcis.get(type);
                    String ltvS = new String(ltv);
                    if (s == null) {
                        uniqueCcis.put(type, ltvS);
                    } else {
                        // if same type exists, replace
                        uniqueCcis.replace(type, ltvS);
                    }
                }
            }
            Set<String> Mds = levelThree.get(i).consolidatedMetadata;
            if (Mds.size() > 0) {
                Iterator<String> itr = Mds.iterator();
                for (int j = 0; itr.hasNext(); j++) {
                    byte[] ltvEntries = itr.next().getBytes();
                    int k = 0;
                    byte length = ltvEntries[k++];
                    byte[] ltv = new byte[length + 1];
                    ltv[0] = length;
                    System.arraycopy(ltvEntries, k, ltv, 1, length);
                    int type = (int) ltv[1];
                    String s = uniqueCcis.get(type);
                    String ltvS = new String(ltv);
                    if (s == null) {
                        uniqueMds.put(type, ltvS);
                    } else {
                        uniqueMds.replace(type, ltvS);
                    }
                }
            }
            levelThree.get(i).consolidatedUniqueMetadata = new HashMap<Integer, String>(uniqueMds);
            levelThree.get(i).consolidatedUniqueCodecInfo =
                    new HashMap<Integer, String>(uniqueCcis);
        }
    }

    static void consolidateBaseofLevelThree(ArrayList<BaseInformation> levelTwo,
            ArrayList<BaseInformation> levelThree, int parentSubgroup, int startIdx, int numNodes) {
        for (int i = startIdx; i < startIdx + numNodes || i < levelThree.size(); i++) {
            levelThree.get(i).subGroupId = levelTwo.get(parentSubgroup).subGroupId;
            log("Copy Codec Id from Level2 Parent" + parentSubgroup);
            System.arraycopy(
                    levelTwo.get(parentSubgroup).consolidatedCodecId,
                    0, levelThree.get(i).consolidatedCodecId, 0, 5);
            // Metadata clone from Parent
            levelThree.get(i).consolidatedMetadata =
                    new LinkedHashSet<String>(levelTwo.get(parentSubgroup).consolidatedMetadata);
            // CCI clone from Parent
            levelThree.get(i).consolidatedCodecInfo =
                    new LinkedHashSet<String>(levelTwo.get(parentSubgroup).consolidatedCodecInfo);
            // Append Level 2 Codec Config
            if (levelThree.get(i).codecConfigLength != 0) {
                log("append level 3 cci to level 3 cons:" + i);
                String s = new String(levelThree.get(i).codecConfigInfo);
                levelThree.get(i).consolidatedCodecInfo.add(s);
            }
        }
    }

    public int getNumberOfIndices() {
        return mNumBISIndices;
    }

    public BaseInformation  getLevelOne() {
        return mLevelOne;
    }

    public ArrayList<BaseInformation> getLevelTwo() {
        return mLevelTwo;
    }

    public ArrayList<BaseInformation> getLevelThree() {
        return mLevelThree;
    }

    public byte getNumberOfSubgroupsofBIG() {
        byte ret = 0;
        if (mLevelOne != null) {
            ret = mLevelOne.numSubGroups;
        }
        return ret;
    }

    public ArrayList<BaseInformation> getBISIndexInfos() {
        return mLevelThree;
    }

    byte[] getMetadata(int subGroup) {
        if (mLevelTwo != null) {
            return mLevelTwo.get(subGroup).metaData;
        }
        return null;
    }

    String getMetadataString(byte[] metadataBytes) {
        String ret = "";
        switch (metadataBytes[1]) {
            case METADATA_LANGUAGE_TYPE:
                char[] lang = new char[3];
                System.arraycopy(metadataBytes, 1, lang, 0, 3);
                Locale locale = new Locale(String.valueOf(lang));
                try {
                    ret = locale.getISO3Language();
                } catch (MissingResourceException e) {
                    ret = "UNKNOWN LANGUAGE";
                }
                break;
            default:
                ret = "UNKNOWN METADATA TYPE";
        }
        log("getMetadataString: " + ret);
        return ret;
    }

    String getCodecParamString(byte[] csiBytes) {
        String ret = "";
        switch (csiBytes[1]) {
            case CODEC_CONFIGURATION_CHANNEL_ALLOCATION_TYPE:
                byte[] location = new byte[4];
                System.arraycopy(csiBytes, 2, location, 0, 4);
                ByteBuffer wrapped = ByteBuffer.wrap(location);
                int audioLocation = wrapped.getInt();
                log("audioLocation: " + audioLocation);
                switch (audioLocation) {
                    case CODEC_AUDIO_LOCATION_FRONT_LEFT:
                        ret = "LEFT";
                        break;
                    case CODEC_AUDIO_LOCATION_FRONT_RIGHT:
                        ret = "RIGHT";
                        break;
                    case CODEC_AUDIO_LOCATION_FRONT_LEFT
                            | CODEC_AUDIO_LOCATION_FRONT_RIGHT:
                        ret = "LR";
                        break;
                }
                break;
            case CODEC_CONFIGURATION_SAMPLE_RATE_TYPE:
                switch (csiBytes[2]) {
                    case CODEC_AUDIO_SAMPLE_RATE_8K:
                        ret = "8K";
                        break;
                    case CODEC_AUDIO_SAMPLE_RATE_16K:
                        ret = "16K";
                        break;
                    case CODEC_AUDIO_SAMPLE_RATE_24K:
                        ret = "24K";
                        break;
                    case CODEC_AUDIO_SAMPLE_RATE_32K:
                        ret = "32K";
                        break;
                    case CODEC_AUDIO_SAMPLE_RATE_44P1K:
                        ret = "44.1K";
                        break;
                    case CODEC_AUDIO_SAMPLE_RATE_48K:
                        ret = "48K";
                        break;
                }
                break;
            case CODEC_CONFIGURATION_FRAME_DURATION_TYPE:
                switch (csiBytes[2]) {
                    case CODEC_AUDIO_FRAME_DURATION_7P5MS:
                        ret = "7.5ms";
                        break;
                    case CODEC_AUDIO_FRAME_DURATION_10MS:
                        ret = "10ms";
                        break;
                }
                break;
            case CODEC_CONFIGURATION_OCTETS_PER_FRAME_TYPE:
                ret = "OPF_" + String.valueOf((int) csiBytes[2]);
                break;
            default:
                ret = "UNKNOWN PARAMETER";
        }
        log("getCodecParamString: " + ret);
        return ret;
    }

    void updateDiffTextforNodes() {
        for (int i = 0; i < mLevelTwo.size(); i++) {
            if (mLevelTwo.get(i).keyMetadataDiff != null) {
                Iterator<String> itr = mLevelTwo.get(i).keyMetadataDiff.iterator();
                for (int k = 0; itr.hasNext(); k++) {
                    mLevelTwo.get(i).diffText =
                            mLevelTwo.get(i)
                                    .diffText
                                    .concat(getMetadataString(itr.next().getBytes()));
                    mLevelTwo.get(i).diffText = mLevelTwo.get(i).diffText.concat("_");
                }
            }
            if (mLevelTwo.get(i).keyCodecCfgDiff != null) {
                Iterator<String> itr = mLevelTwo.get(i).keyCodecCfgDiff.iterator();
                for (int k = 0; itr.hasNext(); k++) {
                    mLevelTwo.get(i).diffText =
                            mLevelTwo.get(i)
                                    .diffText
                                    .concat(getCodecParamString(itr.next().getBytes()));
                    mLevelTwo.get(i).diffText = mLevelTwo.get(i).diffText.concat("_");
                }
            }
        }
        for (int i = 0; i < mLevelThree.size(); i++) {
            if (mLevelThree.get(i).keyCodecCfgDiff != null) {
                Iterator<String> itr = mLevelThree.get(i).keyCodecCfgDiff.iterator();
                for (int k = 0; itr.hasNext(); k++) {
                    mLevelThree.get(i).diffText =
                            mLevelThree
                                    .get(i)
                                    .diffText
                                    .concat(getCodecParamString(itr.next().getBytes()));
                    mLevelThree.get(i).diffText = mLevelThree.get(i).diffText.concat("_");
                }
            }
        }
        // Concat and update the Description
        int startIdx = 0;
        int children = 0;
        for (int i = 0; i < mLevelTwo.size(); i++) {
            startIdx = startIdx + children;
            children = children + mLevelTwo.get(i).numSubGroups;
            for (int j = startIdx;
                    j < startIdx + mLevelTwo.get(i).numSubGroups || j < mLevelThree.size();
                    j++) {
                mLevelThree.get(j).description =
                        mLevelTwo.get(i).diffText + mLevelThree.get(j).diffText;
            }
        }
    }

    void updateUniquenessForLevelTwo(int numNodes) {
        log("updateUniquenessForLevelTwo: ENTER");
        Set<String> uniqueCodecIds = new LinkedHashSet<String>();
        Set<String> uniqueCsfs = new LinkedHashSet<String>();
        Set<String> uniqueMetadatas = new LinkedHashSet<String>();
        log("updateUniquenessForLevelTwo");
        int startIdx = 0;
        int children = 0;
        for (int i = 0; i < mLevelTwo.size(); i++) {
            if (!mLevelTwo.get(i).isCodecIdUnknown()) {
                log("add codecId of subg: " + i);
                String s = new String(mLevelTwo.get(i).codecId);
                uniqueCodecIds.add(s);
            }
            if (mLevelTwo.get(i).codecConfigLength != 0) {
                log("add codecConfig of subg: " + i);
                String s = new String(mLevelTwo.get(i).codecConfigInfo);
                uniqueCsfs.add(s);
            }
            if (mLevelTwo.get(i).metaDataLength != 0) {
                String s = new String(mLevelTwo.get(i).metaData);
                log("add metadata of subg: " + i);
                uniqueMetadatas.add(s);
            }
            startIdx = startIdx + children;
            children = children + mLevelTwo.get(i).numSubGroups;
            updateUniquenessForLevelThree(i, startIdx, mLevelTwo.get(i).numSubGroups);
        }
        Set<String> uniqueCodecParams = new LinkedHashSet<String>();
        Set<String> uniqueMetadataParams = new LinkedHashSet<String>();
        if (uniqueCodecIds.size() > 0) {
            log("LevelTwo: UniqueCodecIds");
        }
        if (uniqueCsfs.size() > 0) {
            log("LevelTwo: uniqueCsfs");
            Iterator<String> itr = uniqueCsfs.iterator();
            for (int i = 0; itr.hasNext(); i++) {
                byte[] ltvEntries = itr.next().getBytes();
                String s = new String(ltvEntries);
                uniqueCodecParams.add(s);
            }
        }
        if (uniqueMetadatas.size() > 0) {
            log("LevelTwo: uniqueMetadatas");
            Iterator<String> itr = uniqueMetadatas.iterator();
            for (int i = 0; itr.hasNext(); i++) {
                byte[] ltvEntries = itr.next().getBytes();
                String s = new String(ltvEntries);
                uniqueMetadataParams.add(s);
            }
        }
        // run though the nodes and update KEY differentiating factors
        if (uniqueCodecParams != null) {
            Iterator<String> itr = uniqueCodecParams.iterator();
            int i = 0;
            for (int k = 0; itr.hasNext(); k++) {
                mLevelTwo.get(i).keyCodecCfgDiff.add(itr.next());
                i = (i + 1) % (numNodes);
            }
        }
        // run though the nodes and update KEY differentiating factors
        if (uniqueMetadataParams != null) {
            Iterator<String> itr = uniqueMetadataParams.iterator();
            int i = 0;
            for (int k = 0; itr.hasNext(); k++) {
                mLevelTwo.get(i).keyMetadataDiff.add(itr.next());
                i = (i + 1) % (numNodes);
            }
        }
    }

    void updateUniquenessForLevelThree(int parentSubgroup, int startIdx, int numNodes) {
        Set<String> uniqueCsfs = new LinkedHashSet<String>();
        log("updateUniquenessForLevelThree: startIdx" + startIdx + "numNodes" + numNodes);
        for (int i = startIdx; i < startIdx + numNodes || i < mLevelThree.size(); i++) {
            if (mLevelThree.get(i).codecConfigLength != 0) {
                String s = new String(mLevelThree.get(i).codecConfigInfo);
                uniqueCsfs.add(s);
                log("LEVEL3: add unique CSFs:");
            }
        }
        Set<String> uniqueCodecParams = new LinkedHashSet<String>();
        if (uniqueCsfs.size() > 0) {
            log("LevelThree: uniqueCsfs");
            Iterator<String> itr = uniqueCsfs.iterator();
            for (int i = 0; itr.hasNext(); i++) {
                byte[] ltvEntries = itr.next().getBytes();

                String s = new String(ltvEntries);
                uniqueCodecParams.add(s);
            }
        }
        // run though the nodes and update KEY differentiating factors
        if (uniqueCodecParams != null) {
            Iterator<String> itr = uniqueCodecParams.iterator();
            int i = startIdx;
            for (int k = 0; itr.hasNext(); k++) {
                mLevelThree.get(i).keyCodecCfgDiff.add(itr.next());
                i = (i + 1) % (startIdx + numNodes);
            }
        }
    }

    void print() {
        mLevelOne.print();
        log("----- Level TWO BASE ----");
        for (int i = 0; i < mLevelTwo.size(); i++) {
            mLevelTwo.get(i).print();
        }
        log("----- Level THREE BASE ----");
        for (int i = 0; i < mLevelThree.size(); i++) {
            mLevelThree.get(i).print();
        }
    }

    static void log(String msg) {
        if (BassConstants.BASS_DBG) {
            Log.d(TAG, msg);
        }
    }
}
