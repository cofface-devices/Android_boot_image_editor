package cfig

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import java.io.File

fun main(args: Array<String>) {
    val log = LoggerFactory.getLogger("Launcher")
    if ((args.size == 6) && args[0] in setOf("pack", "unpack", "sign")) {
        if (args[1] == "vbmeta.img") {
            when (args[0]) {
                "unpack" -> {
                    if (File(UnifiedConfig.workDir).exists()) File(UnifiedConfig.workDir).deleteRecursively()
                    File(UnifiedConfig.workDir).mkdirs()
                    Avb().parseVbMeta(args[1])
                }
                "pack" -> {
                    Avb().packVbMetaWithPadding("/Users/yu/work/boot/avb/avb_test_data/testkey_rsa4096.pem")
                }
                "sign" -> {
                    log.info("vbmeta is already signed")
                }
            }
        } else {
            when (args[0]) {
                "unpack" -> {
                    if (File(UnifiedConfig.workDir).exists()) File(UnifiedConfig.workDir).deleteRecursively()
                    File(UnifiedConfig.workDir).mkdirs()
                    Parser().parseAndExtract(fileName = args[1], avbtool = args[3])
                    Avb().parseVbMeta(args[1])

                    if (File("vbmeta.img").exists()) {
                        Avb().parseVbMeta("vbmeta.img")
                    }
                }
                "pack" -> {
                    Packer().pack(mkbootimgBin = args[2], mkbootfsBin = args[5])
                }
                "sign" -> {
                    Signer.sign(avbtool = args[3], bootSigner = args[4])

                    val readBack = ObjectMapper().readValue(File(UnifiedConfig.workDir + "bootimg.json"),
                            UnifiedConfig::class.java).toArgs()
                    val imgArgs = readBack[0] as ImgArgs
                    val info = readBack[1] as ImgInfo
                    if (imgArgs.verifyType == ImgArgs.VerifyType.AVB) {
                        if (File("vbmeta.img").exists()) {
                            val sig = ObjectMapper().readValue(
                                    Signer.mapToJson(info.signature as LinkedHashMap<*, *>), ImgInfo.AvbSignature::class.java)
                            val newBootImgInfo = Avb().parseVbMeta(args[1] + ".signed")
                            val hashDesc = newBootImgInfo.auxBlob!!.hashDescriptors[0]
                            val origVbMeta = ObjectMapper().readValue(File(Avb.getJsonFileName("vbmeta.img")),
                                    AVBInfo::class.java)
                            for (i in 0..(origVbMeta.auxBlob!!.hashDescriptors.size - 1)) {
                                if (origVbMeta.auxBlob!!.hashDescriptors[i].partition_name == sig.partName) {
                                    val seq = origVbMeta.auxBlob!!.hashDescriptors[i].sequence
                                    origVbMeta.auxBlob!!.hashDescriptors[i] = hashDesc
                                    origVbMeta.auxBlob!!.hashDescriptors[i].sequence = seq
                                }
                            }
                            ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(File(Avb.getJsonFileName("vbmeta.img")), origVbMeta)
                            log.info("vbmeta info updated")
                            Avb().packVbMetaWithPadding("/Users/yu/work/boot/avb/avb_test_data/testkey_rsa4096.pem")
                        } else {
                            //no vbmeta provided
                        }
                    }//end-of-avb
                }//end-of-sign
            }
        }
    } else {
        println("Usage: unpack <boot_image_path> <mkbootimg_bin_path> <avbtool_path> <boot_signer_path> <mkbootfs_bin_path>")
        println("Usage:  pack  <boot_image_path> <mkbootimg_bin_path> <avbtool_path> <boot_signer_path> <mkbootfs_bin_path>")
        println("Usage:  sign  <boot_image_path> <mkbootimg_bin_path> <avbtool_path> <boot_signer_path> <mkbootfs_bin_path>")
        System.exit(1)
    }
}

/*
    (a * x) mod m == 1
 */
//    fun modInv(a: Int, m: Int): Int {
//        for (x in 0 until m) {
//            if (a * x % m == 1) {
//                return x
//            }
//        }
//        throw IllegalArgumentException("modular multiplicative inverse of [$a] under modulo [$m] doesn't exist")
//    }
//
