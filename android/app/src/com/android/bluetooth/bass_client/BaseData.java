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
import java.util.Map;
import java.util.Set;

/**
 * Helper class to parse the Broadcast Announcement BASE data
 */
class BaseData {
    private static final String TAG = "Bassclient-BaseData";
    BaseInformation mLevelOne = new BaseInformation();
    ArrayList<BaseInformation> mLevelTwo = new ArrayList<BaseInformation>();
    ArrayList<BaseInformation> mLevelThree = new ArrayList<BaseInformation>();
    int mNumBISIndices;
    public static byte UNKNOWN_CODEC = (byte) 0xFE;

    public class BaseInformation {
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
            return (codecId != null && codecId[4] == (byte) BaseData.UNKNOWN_CODEC);
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

    BaseData(byte[] serviceData) {
        if (serviceData == null) {
            Log.e(TAG, "Invalid service data for BaseData construction");
            throw new IllegalArgumentException("Basedata: serviceData is null");
        }
        mLevelOne = new BaseInformation();
        mLevelTwo = new ArrayList<BaseInformation>();
        mLevelThree = new ArrayList<BaseInformation>();
        mNumBISIndices = 0;
        log("members initialized");
        log("BASE input" + Arrays.toString(serviceData));

        // Parse Level 1 base
        mLevelOne.level = 1;
        int level1Idx = 0;
        System.arraycopy(serviceData, level1Idx, mLevelOne.presentationDelay, 0, 3);
        level1Idx = level1Idx + 3;
        mLevelOne.numSubGroups = serviceData[level1Idx++];
        mLevelOne.print();
        log("levelOne subgroups" + mLevelOne.numSubGroups);
        int level2Idx = level1Idx;
        for (int i = 0; i < (int) mLevelOne.numSubGroups; i++) {
            log("parsing subgroup" + i);
            BaseInformation b = new BaseInformation();
            b.level = 2;
            b.subGroupId = i;
            b.numSubGroups = serviceData[level2Idx++];
            if (serviceData[level2Idx] == (byte) UNKNOWN_CODEC) {
                // Place It in the last byte of codecID
                System.arraycopy(serviceData, level2Idx, b.codecId, 4, 1);
                level2Idx = level2Idx + 1;
                log("codecId is FE");
            } else {
                System.arraycopy(serviceData, level2Idx, b.codecId, 0, 5);
                level2Idx = level2Idx + 5;
            }
            b.codecConfigLength = serviceData[level2Idx++];
            if (b.codecConfigLength != 0) {
                b.codecConfigInfo = new byte[(int) b.codecConfigLength];
                System.arraycopy(
                        serviceData, level2Idx, b.codecConfigInfo, 0, (int) b.codecConfigLength);
                level2Idx = level2Idx + (int) b.codecConfigLength;
            }
            b.metaDataLength = serviceData[level2Idx++];
            if (b.metaDataLength != 0) {
                b.metaData = new byte[(int) b.metaDataLength];
                System.arraycopy(serviceData, level2Idx, b.metaData, 0, (int) b.metaDataLength);
                level2Idx = level2Idx + (int) b.metaDataLength;
            }
            mNumBISIndices = mNumBISIndices + b.numSubGroups;
            mLevelTwo.add(b);
            b.print();
        }
        // Parse Level 3 Base
        int level3Index = level2Idx;
        for (int k = 0; k < mNumBISIndices; k++) {
            BaseInformation c = new BaseInformation();
            c.level = 3;
            c.index = serviceData[level3Index++];
            c.codecConfigLength = serviceData[level3Index++];
            if (c.codecConfigLength != 0) {
                c.codecConfigInfo = new byte[(int) c.codecConfigLength];
                System.arraycopy(
                        serviceData, level3Index, c.codecConfigInfo, 0, (int) c.codecConfigLength);
                level3Index = level3Index + (int) c.codecConfigLength;
            }
            mLevelThree.add(c);
        }
        consolidateBaseofLevelTwo();
    }

    void consolidateBaseofLevelTwo() {
        int startIdx = 0, children = 0;
        for (int i = 0; i < mLevelTwo.size(); i++) {
            startIdx = startIdx + children;
            children = children + mLevelTwo.get(i).numSubGroups;
            consolidateBaseofLevelThree(i, startIdx, mLevelTwo.get(i).numSubGroups);
        }
        // Eliminate Duplicates at Level 3
        for (int i = 0; i < mLevelThree.size(); i++) {
            Map<Integer, String> uniqueMds = new HashMap<Integer, String>();
            Map<Integer, String> uniqueCcis = new HashMap<Integer, String>();
            Set<String> Csfs = mLevelThree.get(i).consolidatedCodecInfo;
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

            Set<String> Mds = mLevelThree.get(i).consolidatedMetadata;
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
            mLevelThree.get(i).consolidatedUniqueMetadata = new HashMap<Integer, String>(uniqueMds);
            mLevelThree.get(i).consolidatedUniqueCodecInfo =
                    new HashMap<Integer, String>(uniqueCcis);
        }
    }

    void consolidateBaseofLevelThree(int parentSubgroup, int startIdx, int numNodes) {
        for (int i = startIdx; i < startIdx + numNodes || i < mLevelThree.size(); i++) {
            mLevelThree.get(i).subGroupId = mLevelTwo.get(parentSubgroup).subGroupId;
            log("Copy Codec Id from Level2 Parent" + parentSubgroup);
            System.arraycopy(
                    mLevelTwo.get(parentSubgroup).consolidatedCodecId,
                    0, mLevelThree.get(i).consolidatedCodecId, 0, 5);
            // Metadata clone from Parent
            mLevelThree.get(i).consolidatedMetadata =
                    new LinkedHashSet<String>(mLevelTwo.get(parentSubgroup).consolidatedMetadata);
            // CCI clone from Parent
            mLevelThree.get(i).consolidatedCodecInfo =
                    new LinkedHashSet<String>(mLevelTwo.get(parentSubgroup).consolidatedCodecInfo);
            // Append Level 2 Codec Config
            if (mLevelThree.get(i).codecConfigLength != 0) {
                log("append level 3 cci to level 3 cons:" + i);
                String s = new String(mLevelThree.get(i).codecConfigInfo);
                mLevelThree.get(i).consolidatedCodecInfo.add(s);
            }
        }
    }

    public int getNumberOfIndices() {
        return mNumBISIndices;
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
        final int mByte1Language = 0;
        // Different language
        final int mLanguageEnglish = 1;
        final int mLanguageSpanish = 2;
        final int mByte1Description = 1;
        String ret = new String();
        switch (metadataBytes[1]) {
            case mByte1Language:
                switch (metadataBytes[2]) {
                    case mLanguageEnglish:
                        ret = "ENGLISH";
                        break;
                    case mLanguageSpanish:
                        ret = "SPANISH";
                        break;
                    default:
                        ret = "UNKNOWN LANGUAGE";
                        break;
                }
                break;
            default:
                ret = "UNKNOWN";
        }
        log("getMetadataString: " + ret);
        return ret;
    }

    String getCodecParamString(byte[] csiBytes) {
        final int mLocation = 4;
        final int mLeft = 0x01000000;
        final int mRight = 0x02000000;
        String ret = new String();
        // sample rate
        final int mSampleRate = 1;
        // frame duration
        final int mFrameDuration = 2;

        // Octets per frame
        final int mOctetsPerFrame = 8;
        switch (csiBytes[1]) {
            case mLocation:
                byte[] location = new byte[4];
                System.arraycopy(csiBytes, 2, location, 0, 4);
                ByteBuffer wrapped = ByteBuffer.wrap(location);
                int audioLocation = wrapped.getInt();
                log("audioLocation: " + audioLocation);
                switch (audioLocation) {
                    case mLeft:
                        ret = "LEFT";
                        break;
                    case mRight:
                        ret = "RIGHT";
                        break;
                    case mLeft | mRight:
                        ret = "LR";
                        break;
                }
                break;
            case mSampleRate:
                switch (csiBytes[2]) {
                    case 1:
                        ret = "8K";
                        break;
                    case 2:
                        ret = "16K";
                        break;
                    case 3:
                        ret = "24K";
                        break;
                    case 4:
                        ret = "32K";
                        break;
                    case 5:
                        ret = "44.1K";
                        break;
                    case 6:
                        ret = "48K";
                        break;
                }
                break;
            case mFrameDuration:
                switch (csiBytes[2]) {
                    case 1:
                        ret = "FD_1";
                        break;
                }
                break;
            case mOctetsPerFrame:
                switch (csiBytes[2]) {
                    case 28:
                        ret = "OPF_28";
                        break;
                    case 64:
                        ret = "OPF_64";
                        break;
                }
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
