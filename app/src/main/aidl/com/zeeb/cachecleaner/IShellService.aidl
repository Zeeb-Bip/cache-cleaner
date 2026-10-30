package com.zeeb.cachecleaner;

/**
 * AIDL Interface untuk mengeksekusi shell command berprivilese via Shizuku UserService.
 */
interface IShellService {
    /**
     * Menjalankan perintah shell dan mengembalikan output stdout/stderr.
     */
    String runCommand(String cmd);

    /**
     * Mematikan proses UserService saat selesai.
     * Kode transaksi 16777114 adalah konvensi baku Shizuku untuk fungsi destroy.
     */
    void destroy() = 16777114;
}
