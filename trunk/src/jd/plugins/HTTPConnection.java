//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import java.util.List;
import java.util.Map;

public class HTTPConnection {

    public static final int HTTP_NOT_IMPLEMENTED = HttpURLConnection.HTTP_NOT_IMPLEMENTED;
    private HttpURLConnection connection;
    private Map<String, List<String>> requestProperties;
    private boolean connected;
    private String postData;

    public HTTPConnection(URLConnection openConnection) {
        this.connection=(HttpURLConnection)openConnection;
    }

    public void setReadTimeout(int timeout) {
        connection.setReadTimeout(timeout);
        
    }

    public void setConnectTimeout(int timeout) {
        connection.setConnectTimeout(timeout);
        
    }

    public void setInstanceFollowRedirects(boolean redirect) {
        connection.setInstanceFollowRedirects(redirect);
        
    }

    public void setRequestProperty(String key, String value) {
        connection.setRequestProperty(key,value);
        
    }

    public String getHeaderField(String string) {
        return connection.getHeaderField(string);
    }

    public Map<String, List<String>> getHeaderFields() {
        return connection.getHeaderFields();
    }

    public void setDoOutput(boolean b) {
        connection.setDoOutput(b);
        
    }

    public void connect() throws IOException {
        this.requestProperties=connection.getRequestProperties();
        connection.connect();
        this.connected=true;
    }

    public OutputStream getOutputStream() throws IOException {       
        return connection.getOutputStream();
    }

    public int getResponseCode() throws IOException {
        return connection.getResponseCode();
    }

    public InputStream getInputStream() throws IOException {
        return connection.getInputStream();
    }

    public URL getURL() {
       return connection.getURL();
    }

    public String getContentType() {
        return connection.getContentType();
    }

    public HttpURLConnection getHTTPURLConnection() {
        return connection;
      
    }

    public int getContentLength() {
        return connection.getContentLength();
    }

    public Map<String, List<String>> getRequestProperties() {
       try{
           return connection.getRequestProperties();
       }catch(Exception e){
           return this.requestProperties;
       }
     
    }

    public String getRequestProperty(String string) {
       return connection.getRequestProperty(string);
    }

    public void post(String parameter) throws IOException {
        OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
        if (parameter != null) wr.write(parameter);
        this.postData=parameter;
        wr.flush();
        wr.close();
        
    }

    public String getPostData() {
        return postData;
    }

}
