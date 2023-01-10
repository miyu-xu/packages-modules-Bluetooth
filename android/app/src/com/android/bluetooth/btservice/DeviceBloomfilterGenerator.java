/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.bluetooth.btservice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Class to generate a default Device Bloomfilter
 */
public class DeviceBloomfilterGenerator {
    public static final String BLOOM_FILTER_DEFAULT =
            "01070000012cfb062346c2101cfcf541"
            + "29f1ff0404a45c00fac00f5eacbfc5f5"
            + "409eb41b46c4b9b10b12c4d6aaea8819"
            + "2f25a74ad2c1c8db64d1de373a06d578"
            + "51c4367c51725056363e9fc0e71490cc"
            + "c5c297b00614e9177d1ce725f08a6ac3"
            + "5d7f88b7ef96fb85d017f32241045e04"
            + "c4696e355da47723ba896e758ce9a645"
            + "028ac1c2f96a991c7f03f6bed39e3015"
            + "9a449e70b8767ad3894289110866c52c"
            + "bc22fa6add7b98144cb6c63bef17878f"
            + "8071a171896703790018ed46ba160786"
            + "615c83edb86c2466a8e2091dc7120331"
            + "8a4206d7af4fb0e56dd3cc193e0e0316"
            + "a1a3ce411c88c9e102e5b1ae0bd20475"
            + "c35c10e7fa4497844c6d2b6ead0202e2"
            + "6030175afb443e623cf472e1a3164b3a"
            + "62ca2f48c6fa415068a3fb90062f7370"
            + "f631ad541bcdd34947408742e3654e8c"
            + "b25ff09d5a910873237c395913af0980"
            + "44c2a569b85e2e690f0d8e63bf4b0243"
            + "4f0ee3fa01ed6baa9c4182e0f4baf03e"
            + "2b63ec82e8d08435b3c57a3ac0e4fef2"
            + "c1de654648d5615d6044f64a219a01ff"
            + "412887019048f1842370eb332da5a4c2"
            + "63a871c9190ff577944b57160ba848bf"
            + "ae132dce3aeb184c5e2277b20be95b37"
            + "2b8e6ea52a3c9240368d6b8929061daa"
            + "fc691aadd0df330ee24472f8b62125f1"
            + "a35ff823dc0d9a4ecd4b39a75f486902"
            + "189e57990f6c82c995ce2c243c89e280"
            + "7b06aeb4e18a91cab1ce03c3151ea206"
            + "77a36279781eb6aac514482c39f90073"
            + "4c70a2a13c41d39bfa5f9c84506912dc"
            + "3e2a39885dce25047010b1863a4dddab"
            + "2a351d3f1edab178736f41502b3b309a"
            + "4f9b70b78548be0e0e444994caf63073"
            + "093c81bb2a4e27bcec64a8f02b90fb60"
            + "35f1001069e094e3991a00080b8ce278"
            + "63505499fa3a1093ea90dd7a5b826ba5"
            + "860b24607765ea13ea6cff40b1fbd0f3"
            + "7f9c148a7b034442eb3b0fef4f2fa50f"
            + "e898c4f3c4ec9be6ce08e7c99447881f"
            + "6384585139778a5e232c877461c5fae2"
            + "07844119a4202f3ac1d1fa08aaf62c37"
            + "aa42c50a02498217d6fd122d0c112d56"
            + "055bf4342959c5826421e0289a04be37"
            + "f984b13a1a43d2c59edd1510039be9ad"
            + "2aa005218cb223fd09d7cc19242691dd"
            + "7a6dc07328577c98239b6a82221a272c"
            + "b3f1faed763603bcc929568523f81749"
            + "e9e5662a4f3780f50d0dc60940c7d7a4"
            + "ea84b2782274d08ae30c2cd50383d26f"
            + "6a919305395abb4ae0401f2956b5879b"
            + "a7a176d3ba2690191f7715ca5b7e1fc4"
            + "5d09b4aca7f7d7511c5f2d4506ed9474"
            + "c53e08c4b12c312975c2308eb03d4ab6"
            + "52490d038101a3900f2db52962daa38b"
            + "b97d3d41c33852d60bb9646b1dd20dd8"
            + "123777f8873c4883e663fca56cac03d5"
            + "30238cb60c1e4f36ac6b18ceeefa1be6"
            + "aa2515ed3bc0dd7718c7124497755ed2"
            + "82e39af6b2913361010654599e11a4c5"
            + "116651583641a5824316cb72df93698b"
            + "5b42d593032b5e820d253d03461e7752"
            + "57ed51d6506aa61a58a5134808f888e6"
            + "bcde89cee8518b3ccf47886aa89c8d49"
            + "8a56cddada06109b394a8f3f2ac8c53b"
            + "e3de1b15757dc1ff824b717216cb2a0f"
            + "7efaa25b60b571bca426ef769b9aa000"
            + "613632848ba62993f38d800cff036cc9"
            + "72b2c625c24aeb2eaa3f28e2f1d4e4cb"
            + "8e84f43a6d5b6673f6bb8cfc5a2ff319"
            + "5ae1a35e1940431b48c57d5a05b682d6"
            + "58d35abec39224a6816940aae2068141"
            + "7dadc0e13d0118d9caf35d9397bffc6c"
            + "4d567fb16d03823060123e5623b5094e"
            + "434b4dd36500b5e52957a406c442460e"
            + "0c5ae68dfaafb252a9cc6171e72c60e0"
            + "0b4400c7e3a0c8bafd72698c17ab144e"
            + "aaf450dad3f631d75a6852759919bb74"
            + "69e505d97d9ad1f91dd3c56319e25050"
            + "75a02cfc62c2f5abd4e5ca5f760eafe9"
            + "f63627da769b3f301d433054454441fb"
            + "6d1549ea1365ca2610d464c3dc407a0a"
            + "5245278a9aa7359cff21f438b8fc031c"
            + "6ef07b303acfed48e8234341d75468f8"
            + "06aa29462cbdd033f330549d8527980c"
            + "b414008f5d60f06e003165cac610174e"
            + "ad43200b79a1d879b0fe5a1398f556d6"
            + "33c968846cef6801b837811d10334495"
            + "402cb3eb698d51117cf3b4c7a64aa827"
            + "05320a4900191a6dc6193c8b9394d068"
            + "7f9be642b2f8443220c1d1c313665ed4"
            + "7594c7cf86f007bf608dcb33f6901ea6"
            + "5b0a563a66683330451ff349657d6dda"
            + "83554116eae10bc06c942ba54e4520cc"
            + "0027e9938bfcd1325c9ed7e09c851c32"
            + "108bf66a728562164c99e84fc6e4e5be"
            + "f2619b2eea8ff483743041108ea6b155"
            + "5f0a5b854ce37640716b58619e142a02"
            + "6c6400c1a2ab25845e0420b9e95d4350"
            + "ddb380501a2c182504f1941804268be2"
            + "d4b550376c42714c2a708a9faa85fe26"
            + "87c011b8e3b361162092c82f45459678"
            + "3243dc34fb1784dc4db5197b7e31a1f3"
            + "03af7c11ec1a1a4e1a259e6b881ae566"
            + "9bc4ef6fe69a057493167adc0e50e2eb"
            + "5d76a3bf427da88fe47a682fdfd026fb"
            + "376b09edcbb87e41fe0a4919958718e2"
            + "38046cb6fc7829dd0aa60e13027f2af6"
            + "2ca6de3d002330284f7414560c3f91e4"
            + "0c4ce45449f7a0b66c3cde6760c9e668"
            + "a30dffbaed3d3c4fe9d4eb682c0d1631"
            + "64de60a581e000a5536a0d50029b0221"
            + "89734b77feaf96a7e8c8787e71ec91f1"
            + "4ce483998acd89a653e0eba9550ad53c"
            + "383e09e089c9d4e8b7f959435805e797"
            + "230f1eb6ad1ad6f23cc351747218c94b"
            + "395802690405800d43d298f4163eb531"
            + "b71b892f13fbdfcb45dab6b61f73411a"
            + "e9553290eafc08fac95584db1a0e4dad"
            + "60b43604a5137737181931c519fd96d0"
            + "fac0838638851ccc53614f9bf597d7db"
            + "a2d9836e4b34d3ca78ea1b9c31d1ad51"
            + "85bd06b0ded0452533e556fa909bd946"
            + "19f1e184009cbcaff32f4407d9849af5"
            + "19e53e5f699c3e7c267c0416a2f28754"
            + "37756784c7d62d86d96a624395c196b7"
            + "08aa0cee064e8c1de5260717b747caed"
            + "196b1a9c40e699c5a79db43233005ff8"
            + "94e477a046952dd108f144b8d3ad4b9b"
            + "7cc221299e88da8f06367e0bdf0eceb8"
            + "d2aad98672a2d516e5a8f161d9d7f4a4"
            + "6c07841802cc6fb3643e2f0170a1b0b4"
            + "bdea509f678dc4c1861d0b15881ceedf"
            + "20858789d9b6b980f40f94e658d29cb5"
            + "374b48525874f9f15d451e49e071a247"
            + "1451e5261572c003a05c8744be8543dc"
            + "001697881ab27a155c1d6a1c1c217370"
            + "9f790ed2b80c8d750c4ee40b361e35eb"
            + "a98bbe7b5c3e4a8af012cc1c56d6cc9a"
            + "4c8576ca8583ba54192d14ee943a17c4"
            + "fbe9500ce574b8ffb8cf0ae12b5d34d9"
            + "fb8d6de86c0620b728d3cf3457c56931"
            + "2258bfc414d392cbc078628c3f09d8c4"
            + "8ce67e2d530ec5a140ebaa6729733f4d"
            + "b65901d121863481827b1cf99fe7fe4b"
            + "2f6360d084c16fc7d82aac1502655453"
            + "14174444ba72c95356d88f250fb157a6"
            + "9a81e871d114";

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static void generateDefaultBloomfilter(String filePath) throws IOException {
        File outputFile = new File(filePath);
        outputFile.createNewFile(); // if file already exists will do nothing
        FileOutputStream fos = new FileOutputStream(filePath);
        fos.write(hexStringToByteArray(BLOOM_FILTER_DEFAULT));
        fos.close();
    }
}
