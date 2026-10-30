package com.uika.navigator.domain.use_cases

import android.content.Context
import com.uika.navigator.R

class GetDestinationDesc {

    operator fun invoke(number: String, context: Context, bool: Boolean): String {

        var building = ""
        var floor = 0
        val floorStr = context.getString(R.string.floor)

        // Mapping singkatan ke nama lengkap gedung
        val nameMap = mapOf(
            "FTS" to "Fakultas Teknik dan Sains",
            "FIKES" to "Fakultas Ilmu Kesehatan",
            "FH" to "Fakultas Hukum",
            "FEB" to "Fakultas Ekonomi dan Bisnis",
            "FKIP" to "Fakultas Keguruan dan Ilmu Pendidikan",
            "FAI" to "Fakultas Agama Islam",
            "RISE" to "Rise Center",
            "RISE CENTER" to "Rise Center",
            "MASJID" to "Masjid UIKA",
            "BPPSI" to "Gedung BPPSI",
            "PASCA" to "Pascasarjana",
            "PASCASARJANA" to "Pascasarjana"
        )

        val upperNumber = number.uppercase()

        // Jika input bukan angka, cek apakah termasuk mapping gedung
        if (!upperNumber.all { it.isDigit() }) {
            return nameMap[upperNumber] ?: number
        }

        // Penanganan input angka (misal 303, 1101, dst)
        if (number.length == 1) {
            building = context.getString(R.string.main)
            floor = 0
            return "$building, $floorStr$floor"
        } else if (number.length == 3 || number.length == 2) {
            if (number[number.length - 2].isDigit() && number[number.length - 2].digitToInt() > 4) {
                building = context.getString(R.string.lab)
            } else {
                building = context.getString(R.string.main)
            }
            if (number.length == 2) {
                floor = 0
                return "$building, $floorStr$floor"
            }
        } else if (number.length == 4 && number[0].isDigit()) {
            when (number[0]) {
                '1' -> building = context.getString(R.string.first_tower)
                '2' -> building = context.getString(R.string.second_tower)
                '3' -> building = context.getString(R.string.second_building)
                else -> building = context.getString(R.string.unknown_building)
            }
        } else {
            // Jika input tidak dikenali, kembalikan apa adanya
            return number
        }

        // Ambil angka lantai jika ada
        floor = if (number.length >= 3 && number[number.length - 3].isDigit()) {
            number[number.length - 3].digitToInt()
        } else {
            0
        }

        return "$building, $floorStr$floor"
    }
}
