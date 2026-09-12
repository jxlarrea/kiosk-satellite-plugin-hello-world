// SPDX-License-Identifier: Apache-2.0
package me.jxl.kiosk.plugins.hello;

import java.util.Map;

/** Self-contained renderer. KS owns selection, scheduling, input and lifetime. */
final class DvdScreensaver {
    private static String color(Object value, String fallback) {
        return value instanceof String && ((String) value).matches("#[a-fA-F0-9]{6}") ? (String) value : fallback;
    }

    static String document(Map<String, Object> settings) {
        String logo = color(settings.get("dvdLogoColor"), "#00D4FF");
        String background = color(settings.get("dvdBackgroundColor"), "#000000");
        return HTML.replace("LOGOCOLOR", logo).replace("BACKGROUND", background);
    }

    private static final String HTML =
        "<!doctype html>\n" +
        "<html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n" +
        "<style>\n" +
        "html,body{margin:0;width:100%;height:100%;overflow:hidden;background:BACKGROUND}\n" +
        "#logo{position:absolute;left:0;top:0;width:clamp(64px,18vw,240px);max-width:45vw;max-height:45vh;color:LOGOCOLOR;will-change:transform}\n" +
        "svg{display:block;width:100%;height:auto}\n" +
        "</style></head><body>\n" +
        "<div id=\"logo\"><svg viewBox=\"0 0 240 144\" xmlns=\"http://www.w3.org/2000/svg\" role=\"img\" aria-label=\"DVD video\">\n" +
        "<text x=\"112\" y=\"86\" text-anchor=\"middle\" fill=\"currentColor\" font-family=\"Arial,Helvetica,sans-serif\" font-size=\"92\" font-weight=\"900\" font-style=\"italic\" letter-spacing=\"-9\">DVD</text>\n" +
        "<ellipse cx=\"120\" cy=\"109\" rx=\"111\" ry=\"17\" fill=\"currentColor\"/>\n" +
        "<ellipse cx=\"120\" cy=\"109\" rx=\"28\" ry=\"6\" fill=\"BACKGROUND\"/>\n" +
        "<text x=\"124\" y=\"141\" text-anchor=\"middle\" fill=\"currentColor\" font-family=\"Arial,Helvetica,sans-serif\" font-size=\"15\" letter-spacing=\"10\">VIDEO</text>\n" +
        "</svg></div>\n" +
        "<script>\n" +
        "(function () {\n" +
        "  'use strict';\n" +
        "  var logo=document.getElementById('logo'), x=24, y=32, dx=1, dy=1, last=0;\n" +
        "  function frame(now) {\n" +
        "    var dt=last ? Math.min((now-last)/1000,0.05) : 0; last=now;\n" +
        "    var maxX=Math.max(0,innerWidth-logo.offsetWidth), maxY=Math.max(0,innerHeight-logo.offsetHeight);\n" +
        "    x+=dx*110*dt; y+=dy*82*dt;\n" +
        "    if(x>=maxX){x=maxX;dx=-1;} else if(x<=0){x=0;dx=1;}\n" +
        "    if(y>=maxY){y=maxY;dy=-1;} else if(y<=0){y=0;dy=1;}\n" +
        "    logo.style.transform='translate('+x+'px,'+y+'px)';\n" +
        "    requestAnimationFrame(frame);\n" +
        "  }\n" +
        "  document.addEventListener('visibilitychange',function(){last=0;});\n" +
        "  requestAnimationFrame(frame);\n" +
        "}());\n" +
        "</script></body></html>\n";
}
