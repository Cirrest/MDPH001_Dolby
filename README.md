# 水月雨MD-PH-001 DolbyAtmos模块
***
适配水月雨MAID01(MD-PH-001)设备
***

水月雨不按GPL协议开源kernel，没招就自己逆向这个设备kernel、odm、vendor，这个算是自己逆向半年多的成果之一，也算是自己弥补了这个水月雨手机HiFi音乐里面没有杜比全景声的最大遗憾（自认为）</br>

###  *模块内容* 
***
1.手搓DMS/DAX基础音频服务、DAP音效控制器。</br>
2.添加Dolby AC-4/AC-3/E-AC3音频解码器，可亮标并处理输出AppleMusic、QQ音乐、哔哩哔哩、iQIYI等软件的杜比全景声音频</br>
3.移植并适调iPad同款DolbyAtmos双耳空间渲染，闻所未闻的感染力，极为震撼。</br>
4.已做兼容性处理，适配100级音量调节及曲线，可与原厂AudioConsloe同开同用，DAP音效控制器内全局Dolby处理关闭后即可恢复原厂全局SRC绕过。</br>
5.扬声器、3.5mm、4.4mm均可享受DolbyAtmos输出。</br>

###  *要求* 
***
1.设备必须为水月雨MD-PH-001原厂ROM并已修补好Magisk。</br>
2.Android版本为14及以上。</br>
3.使用官方Magisk刷入模块。</br>

###  *注意* 
***
1.杜比全景声控制软件为MD-PH-001 Dolby Control</br>
2.打开Dolby全局处理后，全局音频输出将会以16bit/24bit自适应位深-48Khz PCM输出，关闭即可恢复原厂全局SRC绕过</br>
3.可随时播放AC4/AC3/E-AC3编码音频，不受Dolby全局处理开关限制（前提软件调用系统音频解码器）。播放该编码音频将会使用音频编码声明的音频模式均衡器，不使用Dolby Control内设定的自定义音频模式均衡器。</br>
4.尽量不要装其它的音频模块，以免冲突</br>
5.涉及杜比认证原因，仅模块本身及自制DolbyControl进行GPL开源，不涉及Dolby、编码器开源。</br>
6.模块免费，禁止任何形式商业化、收费、二改，所有文件均已打上数字水印。</br>
7.欢迎宣传，转载请注明出处。</br>

###  *安全保证* 
***
该模块保证不含格机脚本、远程遥测等危险操作/后门，安装前请核对SHA256及MD5以免刷到非本人制作模块。</br>
---
<img width="3240" height="2400" alt="im1" src="https://github.com/user-attachments/assets/aaa9dceb-9c35-49e2-89b0-72a82a7e4974" />
<img width="2886" height="1080" alt="im2" src="https://github.com/user-attachments/assets/54bd50c9-548c-4796-83e3-d409d8ce10bb" />
<img width="1152" height="1152" alt="打赏作者，万分感谢" src="https://github.com/user-attachments/assets/456e042d-d526-4d44-825c-d702fa3167ff" />

---
[控制器使用Material Design 3(Material You)风格设计](https://m3.material.io/)
