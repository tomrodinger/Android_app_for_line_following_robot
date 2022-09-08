package com.hani.btapp.core

import android.content.Context
import com.hani.btapp.Logger
import com.hani.btapp.utils.toBytes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.zip.CRC32
import javax.inject.Inject
import javax.inject.Singleton

private const val FLASH_TOTAL_SIZE = 0xca000

/**
 * Class that will pad and append boot header on the firmware file.
 *
 * Created by hanif on 2022-08-10.
 */
@Singleton
class FinalFirmwareCreator @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun createFirmwareWithBootHeader(data: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            create(data)
        }
    }

    private fun create(firmwareData: ByteArray): Boolean {
        var data = firmwareData.copyOf()

        // pad 0x00 until the length of the data is divisible by 16
        while (data.size % 0x10 != 0) {
            data += 0x0
        }

        // Add boot header
        val config = ConfigParser()
        try {
            config.read(context.assets.open("bootheader_cfg.ini"))
        } catch (e: ConfigParseException) {
            Logger.log("Error: Could not parse boot header config: $e")
            return false
        }
        data = getBootHeader(data, config) + data

        // pad 0xFF until the length of the data is divisible by 256
        while (data.size and 0xFF != 0) {
            data += -1
        }
        if (data.size > FLASH_TOTAL_SIZE) {
            Logger.log("Error: the firmware is too big to fit in the flash")
            return false
        }

        val output = File(context.filesDir, "firmwarewbootheader.bin")
        if (output.exists()) {
            output.delete()
        }
        output.writeBytes(data)
        Logger.log("Created firmware with boot header. Length: ${data.size} bytes.")

        return true
    }

    private fun getBootHeader(data: ByteArray, config: ConfigParser): ByteArray {
        // Boot header
        var header = Integer.decode(config.get("BOOTHEADER_CFG", "magic_code")).toBytes(4)
        header += Integer.decode(config.get("BOOTHEADER_CFG", "revision")).toBytes(4)

        // Flash configuration
        var flashCfg = Integer.decode(config.get("BOOTHEADER_CFG", "io_mode")).toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "cont_read_support").toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "sfctrl_clk_delay").toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "sfctrl_clk_invert")).toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "reset_en_cmd")).toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "reset_cmd")).toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "exit_contread_cmd")).toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "exit_contread_cmd_size").toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "jedecid_cmd")).toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "jedecid_cmd_dmy_clk").toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "qpi_jedecid_cmd")).toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "qpi_jedecid_dmy_clk").toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "sector_size").toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "mfg_id")).toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "page_size").toBytes(2)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "chip_erase_cmd")).toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "sector_erase_cmd")).toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "blk32k_erase_cmd")).toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "blk64k_erase_cmd")).toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "write_enable_cmd")).toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "page_prog_cmd")).toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "qpage_prog_cmd")).toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "qual_page_prog_addr_mode").toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "fast_read_cmd")).toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "fast_read_dmy_clk").toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "qpi_fast_read_cmd")).toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "qpi_fast_read_dmy_clk").toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "fast_read_do_cmd")).toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "fast_read_do_dmy_clk").toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "fast_read_dio_cmd")).toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "fast_read_dio_dmy_clk").toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "fast_read_qo_cmd")).toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "fast_read_qo_dmy_clk").toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "fast_read_qio_cmd")).toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "fast_read_qio_dmy_clk").toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "qpi_fast_read_qio_cmd")).toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "qpi_fast_read_qio_dmy_clk").toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "qpi_page_prog_cmd")).toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "write_vreg_enable_cmd")).toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "wel_reg_index").toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "qe_reg_index").toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "busy_reg_index").toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "wel_bit_pos").toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "qe_bit_pos").toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "busy_bit_pos").toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "wel_reg_write_len").toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "wel_reg_read_len").toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "qe_reg_write_len").toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "qe_reg_read_len").toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "release_power_down")).toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "busy_reg_read_len").toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "reg_read_cmd0")).toBytes(2)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "reg_read_cmd1")).toBytes(2)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "reg_write_cmd0")).toBytes(2)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "reg_write_cmd1")).toBytes(2)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "enter_qpi_cmd")).toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "exit_qpi_cmd")).toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "cont_read_code")).toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "cont_read_exit_code")).toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "burst_wrap_cmd")).toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "burst_wrap_dmy_clk")).toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "burst_wrap_data_mode").toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "burst_wrap_code")).toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "de_burst_wrap_cmd")).toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "de_burst_wrap_cmd_dmy_clk"))
            .toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "de_burst_wrap_code_mode").toBytes(1)
        flashCfg += Integer.decode(config.get("BOOTHEADER_CFG", "de_burst_wrap_code")).toBytes(1)
        flashCfg += config.getInt("BOOTHEADER_CFG", "sector_erase_time").toBytes(2)
        flashCfg += config.getInt("BOOTHEADER_CFG", "blk32k_erase_time").toBytes(2)
        flashCfg += config.getInt("BOOTHEADER_CFG", "blk64k_erase_time").toBytes(2)
        flashCfg += config.getInt("BOOTHEADER_CFG", "page_prog_time").toBytes(2)
        flashCfg += config.getInt("BOOTHEADER_CFG", "chip_erase_time").toBytes(2)
        flashCfg += config.getInt("BOOTHEADER_CFG", "power_down_delay").toBytes(2)
        flashCfg = Integer.decode(config.get("BOOTHEADER_CFG", "flashcfg_magic_code")).toBytes(4) +
                flashCfg +
                getCRC32(flashCfg).toBytes(4)

        // Clock configuration
        var clockCfg = config.getInt("BOOTHEADER_CFG", "xtal_type").toBytes(1)
        clockCfg += config.getInt("BOOTHEADER_CFG", "pll_clk").toBytes(1)
        clockCfg += config.getInt("BOOTHEADER_CFG", "hclk_div").toBytes(1)
        clockCfg += config.getInt("BOOTHEADER_CFG", "bclk_div").toBytes(1)
        clockCfg += config.getInt("BOOTHEADER_CFG", "flash_clk_type").toBytes(2)
        clockCfg += config.getInt("BOOTHEADER_CFG", "flash_clk_div").toBytes(2)
        clockCfg = Integer.decode(config.get("BOOTHEADER_CFG", "clkcfg_magic_code")).toBytes(4) +
                clockCfg +
                getCRC32(clockCfg).toBytes(4)

        // Boot configuration
        var bootCfg = ((config.getInt("BOOTHEADER_CFG", "sign") or config.getInt(
            "BOOTHEADER_CFG",
            "encrypt_type"
        ) shl 2) or
                (config.getInt("BOOTHEADER_CFG", "key_sel") shl 4) or (config.getInt(
            "BOOTHEADER_CFG",
            "no_segment"
        ) shl 8) or
                (config.getInt("BOOTHEADER_CFG", "cache_enable") shl 9) or (config.getInt(
            "BOOTHEADER_CFG",
            "notload_in_bootrom"
        ) shl 10) or
                (config.getInt("BOOTHEADER_CFG", "aes_region_lock") shl 11) or (Integer.decode(
            config.get(
                "BOOTHEADER_CFG",
                "cache_way_disable"
            )
        ) shl 12) or
                (config.getInt("BOOTHEADER_CFG", "crc_ignore") shl 16) or (config.getInt(
            "BOOTHEADER_CFG",
            "hash_ignore"
        ) shl 17) or
                (config.getInt("BOOTHEADER_CFG", "boot2_enable") shl 19)).toBytes(4)

        // Image configuration
        var imageCfg = data.size.toBytes(4)
        imageCfg += config.getInt("BOOTHEADER_CFG", "bootentry").toBytes(4)
        imageCfg += Integer.decode(config.get("BOOTHEADER_CFG", "img_start")).toBytes(4)

        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(data)

        imageCfg += messageDigest.digest()
        imageCfg += Integer.decode(config.get("BOOTHEADER_CFG", "boot2_pt_table_0")).toBytes(4)
        imageCfg += Integer.decode(config.get("BOOTHEADER_CFG", "boot2_pt_table_1")).toBytes(4)

        header += flashCfg + clockCfg + bootCfg + imageCfg
        header += getCRC32(header).toBytes(4)

        while (header.size < 0x1000) {
            header += -1
        }

        return header
    }

    private fun getCRC32(byteArray: ByteArray): Long {
        val crc = CRC32()
        crc.update(byteArray)
        return crc.value
    }

}