package site.wikics.reader;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Shared native design tokens and small line icons. No remote fonts or UI assets. */
final class Ui {
    final Context context;final boolean dark;
    final int bg,card,ink,muted,accent,soft,border;
    Ui(Context context,boolean dark){this.context=context;this.dark=dark;bg=Color.parseColor(dark?"#15141B":"#F8F7FC");card=Color.parseColor(dark?"#211E2A":"#FFFFFF");ink=Color.parseColor(dark?"#EEEAF6":"#242130");muted=Color.parseColor(dark?"#B7AFC7":"#716B80");accent=Color.parseColor(dark?"#C1B2FF":"#6655CC");soft=Color.parseColor(dark?"#30283F":"#EDE8FC");border=Color.parseColor(dark?"#34303F":"#E5E1EF");}
    int dp(float n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
    LinearLayout column(){LinearLayout v=new LinearLayout(context);v.setOrientation(LinearLayout.VERTICAL);return v;}
    LinearLayout row(){LinearLayout v=new LinearLayout(context);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    TextView text(String value,int sp,int color,boolean bold){TextView t=new TextView(context);t.setText(value);t.setTextSize(sp);t.setTextColor(color);t.setFontFeatureSettings("kern");t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));t.setIncludeFontPadding(false);return t;}
    TextView label(String value){TextView t=text(value,11,muted,true);t.setLetterSpacing(.10f);return t;}
    GradientDrawable shape(int color,int radius,boolean outline){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(outline)d.setStroke(dp(1),border);return d;}
    void surface(View view,int color,int radius,boolean outline,boolean ripple){GradientDrawable bg=shape(color,radius,outline);view.setBackground(ripple?new RippleDrawable(ColorStateList.valueOf(dark?0x30FFFFFF:0x186655CC),bg,shape(Color.WHITE,radius,false)):bg);}
    void padding(View v,int l,int t,int r,int b){v.setPadding(dp(l),dp(t),dp(r),dp(b));}
    void gap(LinearLayout parent,int height){parent.addView(new View(context),new LinearLayout.LayoutParams(1,dp(height)));}
    TextView button(String value,boolean primary,Runnable click){TextView t=text(value,15,primary?(dark?bg:Color.WHITE):accent,true);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(52));padding(t,18,12,18,12);surface(t,primary?accent:soft,16,false,true);t.setOnClickListener(v->click.run());t.setFocusable(true);return t;}
    View iconButton(String name,String description,Runnable click){FrameLayout box=new FrameLayout(context);box.setMinimumHeight(dp(48));box.setMinimumWidth(dp(48));Icon icon=new Icon(context,name,ink);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(23),dp(23),Gravity.CENTER);box.addView(icon,p);surface(box,Color.TRANSPARENT,24,false,true);box.setContentDescription(description);box.setFocusable(true);box.setOnClickListener(v->click.run());return box;}
    Icon icon(String name,int color){return new Icon(context,name,color);}
    final class Icon extends View {
        final String name;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Icon(Context c,String name,int color){super(c);this.name=name;p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.65f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas original){super.onDraw(original);original.save();original.scale(getWidth()/24f,getHeight()/24f);Canvas c=original;Path path=new Path();
            switch(name){
                case "book":path.moveTo(3,4);path.lineTo(9,5);path.quadTo(12,5,12,8);path.quadTo(12,5,15,5);path.lineTo(21,4);path.lineTo(21,19);path.lineTo(15,20);path.quadTo(12,20,12,22);path.quadTo(12,20,9,20);path.lineTo(3,19);path.close();c.drawPath(path,p);c.drawLine(12,8,12,21,p);break;
                case "saved":case "savedFill":path.moveTo(6,3);path.lineTo(18,3);path.lineTo(18,21);path.lineTo(12,17);path.lineTo(6,21);path.close();if(name.equals("savedFill"))p.setStyle(Paint.Style.FILL);c.drawPath(path,p);break;
                case "search":c.drawCircle(10,10,6.5f,p);c.drawLine(15,15,21,21,p);break;
                case "back":path.moveTo(14,5);path.lineTo(7,12);path.lineTo(14,19);c.drawPath(path,p);break;
                case "arrow":path.moveTo(9,6);path.lineTo(15,12);path.lineTo(9,18);c.drawPath(path,p);break;
                case "refresh":c.drawArc(4,4,20,20,40,280,false,p);path.moveTo(20,3);path.lineTo(20,9);path.lineTo(14,9);c.drawPath(path,p);break;
                case "settings":c.drawLine(4,6,20,6,p);c.drawLine(4,12,20,12,p);c.drawLine(4,18,20,18,p);c.drawCircle(9,6,2,p);c.drawCircle(16,12,2,p);c.drawCircle(8,18,2,p);break;
                case "more":p.setStyle(Paint.Style.FILL);for(int y=5;y<=19;y+=7)c.drawCircle(12,y,1.6f,p);break;
                case "clock":c.drawCircle(12,12,8,p);c.drawLine(12,7,12,12,p);c.drawLine(12,12,16,14,p);break;
                case "check":path.moveTo(5,12);path.lineTo(10,17);path.lineTo(20,7);c.drawPath(path,p);break;
                case "external":c.drawLine(11,5,5,5,p);c.drawLine(5,5,5,19,p);c.drawLine(5,19,19,19,p);c.drawLine(19,19,19,13,p);c.drawLine(12,12,21,3,p);c.drawLine(15,3,21,3,p);c.drawLine(21,3,21,9,p);break;
                default:c.drawCircle(12,12,8,p);c.drawLine(12,7,12,12,p);c.drawPoint(12,16,p);
            }original.restore();
        }
    }
}
