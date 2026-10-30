package com.zeeb.cachecleaner;

interface IShellService {
    String runCommand(String cmd) = 1;
    void destroy() = 16777114;
}
