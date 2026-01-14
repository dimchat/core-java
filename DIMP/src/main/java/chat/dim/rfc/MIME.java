/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.rfc;


/**
 *  RFC 2045
 *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *  https://www.rfc-editor.org/rfc/rfc2045
 *
 *      MIME: Multipurpose Internet Mail Extensions
 */
public interface MIME {

    //
    //  type
    //
    String TEXT         = "text";
    String IMAGE        = "image";
    String AUDIO        = "audio";
    String VIDEO        = "video";
    String APP          = "application";

    //
    //  subtype
    //
    String PLAIN        = "plain";
    String HTML         = "html";
    String XML          = "xml";
    String CSS          = "css";
    String JS           = "javascript";

    String BMP          = "bmp";
    String GIF          = "gif";
    String PNG          = "png";
    String JPG          = "jpeg";
    String X_ICON       = "x-icon";
    String SVG_XML      = "svg+xml";
    String WEB_P        = "webp";

    String WAV          = "wav";
    String OGG          = "ogg";
    String MP3          = "mp3";
    String MP4          = "mp4";
    String MPG          = "mpeg";
    String WEB_M        = "webm";

    String PDF          = "pdf";
    String MS_WORD      = "msword";
    String MS_EXCEL     = "vnd.ms-excel";
    String MS_PPT       = "vnd.ms-powerpoint";
    String ZIP          = "zip";
    String JSON         = "json";
    String OCTET_STREAM = "octet-stream";

    /**
     *  "Content-Type" ":" type "/" subtype
     */
    interface ContentType {

        //
        //  text/*
        //
        String TEXT_PLAIN       = TEXT + "/" + PLAIN;  //  text/plain
        String TEXT_HTML        = TEXT + "/" + HTML;   //  text/html
        String TEXT_XML         = TEXT + "/" + XML;    //  text/xml
        String TEXT_CSS         = TEXT + "/" + CSS;    //  text/css
        String TEXT_JS          = TEXT + "/" + JS;     //  text/javascript

        //
        //  image/*
        //
        String IMAGE_BMP        = IMAGE + "/" + BMP;      //  image/bmp
        String IMAGE_GIF        = IMAGE + "/" + GIF;      //  image/gif
        String IMAGE_PNG        = IMAGE + "/" + PNG;      //  image/png
        String IMAGE_JPG        = IMAGE + "/" + JPG;      //  image/jpeg
        String IMAGE_X_ICON     = IMAGE + "/" + X_ICON;   //  image/x-icon
        String IMAGE_SVG_XML    = IMAGE + "/" + SVG_XML;  //  image/svg+xml
        String IMAGE_WEB_P      = IMAGE + "/" + WEB_P;    //  image/webp

        //
        //  audio/*
        //
        String AUDIO_WAV        = AUDIO + "/" + WAV;  //  audio/wav
        String AUDIO_OGG        = AUDIO + "/" + OGG;  //  audio/ogg
        String AUDIO_MP3        = AUDIO + "/" + MP3;  //  audio/mp3
        String AUDIO_MP4        = AUDIO + "/" + MP4;  //  audio/mp4
        String AUDIO_MPG        = AUDIO + "/" + MPG;  //  audio/mpeg

        //
        //  video/*
        //
        String VIDEO_MP4        = VIDEO + "/" + MP4;    //  video/mp4
        String VIDEO_MPG        = VIDEO + "/" + MPG;    //  video/mpeg
        String VIDEO_OGG        = VIDEO + "/" + OGG;    //  video/ogg
        String VIDEO_WEB_M      = VIDEO + "/" + WEB_M;  //  video/webm

        //
        //  application/*
        //
        String APP_PDF          = APP + "/" + PDF;           //  application/pdf
        String APP_MS_WORD      = APP + "/" + MS_WORD;       //  application/msword
        String APP_MS_EXCEL     = APP + "/" + MS_EXCEL;      //  application/vnd.ms-excel
        String APP_MS_PPT       = APP + "/" + MS_PPT;        //  application/vnd.ms-powerpoint
        String APP_ZIP          = APP + "/" + ZIP;           //  application/zip
        String APP_XML          = APP + "/" + XML;           //  application/xml
        String APP_JSON         = APP + "/" + JSON;          //  application/json
        String APP_OCTET_STREAM = APP + "/" + OCTET_STREAM;  //  application/octet-stream

        // ...

    }

}
