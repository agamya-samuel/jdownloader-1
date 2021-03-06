//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fshare.vn", "mega.1280.com" }, urls = { "http://(www\\.)?(mega\\.1280\\.com|fshare\\.vn)/file/[0-9A-Z]+", "dgjediz65854twsdfbtzoi6UNUSED_REGEX" }, flags = { 2, 0 })
public class FShareVn extends PluginForHost {

    private static final String  SERVERERROR = "Tài nguyên bạn yêu cầu không tìm thấy";

    private static final String  IPBLOCKED   = ">Vui lòng chờ lượt download kế tiếp";

    private static AtomicInteger maxPrem     = new AtomicInteger(1);

    public FShareVn(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.fshare.vn/buyacc.php");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("mega.1280.com", "fshare.vn"));
    }

    public void doFree(DownloadLink downloadLink) throws Exception {
        if (br.containsHTML(IPBLOCKED)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
        br.setFollowRedirects(true);
        br.setDebug(true);
        final String fid = br.getRegex("name=\"file_id\" value=\"(\\d+)\"").getMatch(0);
        if (fid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.postPage(downloadLink.getDownloadURL(), "link_file_pwd_dl=&action=download_file&file_id=" + fid);
        if (br.containsHTML(IPBLOCKED)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            rc.setCode(c);
            if (br.containsHTML("frm_download")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String downloadURL = br.getRegex("window\\.location=\\'(.*?)\\'").getMatch(0);
        if (downloadURL == null) {
            downloadURL = br.getRegex("value=\"Download\" name=\"btn_download\" value=\"Download\"  onclick=\"window\\.location=\\'(http://.*?)\\'\"").getMatch(0);
            if (downloadURL == null) {
                downloadURL = br.getRegex("\\'(http://download\\d+\\.fshare\\.vn/download/[A-Za-z0-9]+/.*?)\\'").getMatch(0);
            }
        }
        // Waittime
        String wait = br.getRegex("var count = (\\d+);").getMatch(0);
        if (wait == null || downloadURL == null || !downloadURL.contains("fshare.vn")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        sleep(Long.parseLong(wait) * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadURL, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(SERVERERROR)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.fsharevn.Servererror", "Servererror!"), 60 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage("http://www.fshare.vn/index.php");
        // >Hạn dùng:<strong class="color_red">20-12-2012</strong>
        String validUntil = br.getRegex(">Hạn dùng:<strong class=\"color_red\">(\\d+\\-\\d+\\-\\d+)</strong>").getMatch(0);
        if (validUntil != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntil, "MM-dd-yyyy", Locale.ENGLISH));
            try {
                maxPrem.set(-1);
                account.setMaxSimultanDownloads(-1);
            } catch (final Throwable e) {
            }
            ai.setStatus("Premium User");
            account.setProperty("acctype", Property.NULL);
        } else {
            try {
                maxPrem.set(1);
                account.setMaxSimultanDownloads(1);
            } catch (final Throwable e) {
            }
            ai.setStatus("Registered (free) User");
            account.setProperty("acctype", "free");
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.fshare.vn/policy.php?action=sudung";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.getPage(link.getDownloadURL());
        if (account.getStringProperty("acctype") != null) {
            doFree(link);
        } else {
            if (br.getRedirectLocation() != null) {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, br.getRedirectLocation(), true, 1);
            } else {
                Form dlForm = br.getFormbyProperty("name", "frm_download");
                if (dlForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlForm, true, 1);
            }
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (br.containsHTML(SERVERERROR)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.fsharevn.Servererror", "Servererror!"), 60 * 60 * 1000l);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage("https://www.fshare.vn/login.php");
        br.postPage("https://www.fshare.vn/login.php", "login_useremail=" + Encoding.urlEncode(account.getUser()) + "&login_password=" + Encoding.urlEncode(account.getPass()) + "&url_refe=http%3A%2F%2Fwww.fshare.vn%2Flogin.php&auto_login=1");
        if (br.getCookie("https://www.fshare.vn", "fshare_userpass") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Sometime the page is extremely slow!
        br.setReadTimeout(120 * 1000);
        // br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:7.0.1) Gecko/20100101 Firefox/7.0.1");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        br.getHeaders().put("Accept-Encoding", "gzip, deflate");
        br.setCustomCharset("UTF-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(<title>Fshare \\– Dịch vụ chia sẻ số 1 Việt Nam \\– Cần là có \\- </title>|b>Liên kết bạn chọn không tồn tại trên hệ thống Fshare</|<li>Liên kết không chính xác, hãy kiểm tra lại|<li>Liên kết bị xóa bởi người sở hữu\\.<)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<p><b>Tên file:</b> (.*?)</p>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?) \\- Fshare \\- Dịch vụ chia sẻ, lưu trữ dữ liệu miễn phí tốt nhất </title>").getMatch(0);
        String filesize = br.getRegex("<p><b>Dung lượng: </b>(.*?)</p>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Server sometimes sends bad filenames
        link.setFinalFileName(Encoding.htmlDecode(filename));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}