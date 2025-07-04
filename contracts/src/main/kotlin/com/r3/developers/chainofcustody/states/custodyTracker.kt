package com.r3.developers.chainofcustody.states

import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.types.MemberX500Name
import java.time.Instant

// Custody interaction merupakan data class yang akan mendefinisikan bagaimana interaksi yang dilakukan pada chain of custody (serangkaian laporan yang berkaitan dengan bukti digital)
@CordaSerializable
data class CustodyInteraction(
    // jenis laporan yang dibuat
    val typeReport: String,
    // nama petugas yang melakukan interaksi
    val officerName: MemberX500Name,
    // jenis dan deskripsi interaksi yang dilakukan
    val interaction: String,
    // kapan interaksi dilakukan
    val timestamp: Instant,
    // variabel yang menyimpan nama pemegang baru dari suatu laporan, hanya digunakan saat fungsi transfer dipanggil
    val newHolder: MemberX500Name? = null
)
