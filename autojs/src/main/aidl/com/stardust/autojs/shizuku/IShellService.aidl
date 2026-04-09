package com.stardust.autojs.shizuku;

import com.stardust.autojs.shizuku.Result;

interface IShellService {
    void destroy();
    Result exec(String command);
}
