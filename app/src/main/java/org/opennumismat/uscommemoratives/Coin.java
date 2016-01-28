package org.opennumismat.uscommemoratives;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by v.ignatov on 20.10.2014.
 */
public class Coin implements Parcelable {
    private static final int ID_COLUMN = 0;
    private static final int TITLE_COLUMN = 1;
    private static final int UNIT_COLUMN = 2;
    private static final int YEAR_COLUMN = 3;
    private static final int COUNTRY_COLUMN = 4;
    private static final int MINTMARK_COLUMN = 5;
    private static final int MINTAGE_COLUMN = 6;
    private static final int SERIES_COLUMN = 7;
    private static final int SUBJECT_SHORT_COLUMN = 8;
    public static final int IMAGE_COLUMN = 9;

    private static final int SUBJECT_EX_COLUMN = 0;
    private static final int DATE_EX_COLUMN = 1;
    private static final int MINT_EX_COLUMN = 2;
    private static final int MATERIAL_EX_COLUMN = 3;
    private static final int OBVERSE_IMAGE_EX_COLUMN = 4;
    private static final int REVERSE_IMAGE_EX_COLUMN = 5;

    private boolean use_mint;
    private long id;
    public String title;
    public String unit;
    public String country;
    public long year;
    public String mintmark;
    public String mint;
    public long mintage;
    public String series;
    public byte[] image;
    public String subject;
    public String subject_short;
    public String material;
    public String date;
    public byte[] obverse_image;
    public byte[] reverse_image;
    public int grade;
    public long count;
    public int count_unc;
    public int count_au;
    public int count_xf;
    public int count_vf;
    public int count_f;
    public String price_unc;
    public String price_xf;
    public String price_vf;
    public String price_f;

    public Coin(Cursor cursor, boolean use_mint) {
        this.use_mint = use_mint;

        id = cursor.getLong(ID_COLUMN);
        if (cursor.isNull(TITLE_COLUMN))
            title = "";
        else
            title = cursor.getString(TITLE_COLUMN);
        if (cursor.isNull(UNIT_COLUMN))
            unit = "";
        else
            unit = cursor.getString(UNIT_COLUMN);
        if (cursor.isNull(COUNTRY_COLUMN))
            country = "";
        else
            country = cursor.getString(COUNTRY_COLUMN);
        if (cursor.isNull(YEAR_COLUMN))
            year = 0;
        else
            year = cursor.getLong(YEAR_COLUMN);
        if (!use_mint || cursor.isNull(MINTMARK_COLUMN))
            mintmark = "";
        else
            mintmark = cursor.getString(MINTMARK_COLUMN);
        if (!use_mint || cursor.isNull(MINTAGE_COLUMN))
            mintage = 0;
        else
            mintage = cursor.getLong(MINTAGE_COLUMN);
        if (cursor.isNull(SERIES_COLUMN))
            series = "";
        else
            series = cursor.getString(SERIES_COLUMN);
        if (cursor.isNull(SUBJECT_SHORT_COLUMN))
            subject_short = "";
        else
            subject_short = cursor.getString(SUBJECT_SHORT_COLUMN);

        count = count_unc = count_au = count_xf = count_vf = count_f = 0;
        price_unc = price_xf = price_vf = price_f = "";
        grade = SqlAdapter.GRADE_DEFAULT;
    }

    private Coin(Parcel in) {
        int length;

        id = in.readLong();
        title = in.readString();
        unit = in.readString();
        country = in.readString();
        year = in.readLong();
        mintmark = in.readString();
        mint = in.readString();
        mintage = in.readLong();
        series = in.readString();
        subject = in.readString();
        subject_short = in.readString();
        material = in.readString();
        date = in.readString();
        count = in.readLong();
        price_unc = in.readString();
        price_xf = in.readString();
        price_vf = in.readString();
        price_f = in.readString();

        length = in.readInt();
        if (length > 0) {
            obverse_image = new byte[length];
            in.readByteArray(obverse_image);
        }
        length = in.readInt();
        if (length > 0) {
            reverse_image = new byte[length];
            in.readByteArray(reverse_image);
        }
    }

    public static final Parcelable.Creator<Coin> CREATOR
            = new Parcelable.Creator<Coin>() {
        public Coin createFromParcel(Parcel in) {
            return new Coin(in);
        }

        public Coin[] newArray(int size) {
            return new Coin[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(id);
        out.writeString(title);
        out.writeString(unit);
        out.writeString(country);
        out.writeLong(year);
        out.writeString(mintmark);
        out.writeString(mint);
        out.writeLong(mintage);
        out.writeString(series);
        out.writeString(subject);
        out.writeString(subject_short);
        out.writeString(material);
        out.writeString(date);
        out.writeLong(count);
        out.writeString(price_unc);
        out.writeString(price_xf);
        out.writeString(price_vf);
        out.writeString(price_f);
        if (obverse_image != null) {
            out.writeInt(obverse_image.length);
            out.writeByteArray(obverse_image);
        }
        else {
            out.writeInt(0);
        }
        if (reverse_image != null) {
            out.writeInt(reverse_image.length);
            out.writeByteArray(reverse_image);
        }
        else {
            out.writeInt(0);
        }
    }

    public long getId() {
        return id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        if (mintmark.isEmpty())
            return year + " " + title;
        else
            return year + "-" + mintmark + " " + title;
    }
    public String getCountry() {
        return country;
    }
    public String getSubject() {
        return subject;
    }
    public String getSubjectShort() {
        return subject_short;
    }
    public String getSeries() {
        return series;
    }

    public String getDescription(Context context) {
        String desc = "";
        if (!subject_short.isEmpty())
            desc += subject_short + ", ";
        desc += unit;
        if (year > 0)
            desc += ", " + year;
        if (!mintmark.isEmpty())
            desc += ", " + mintmark;
        if (mintage > 0)
            desc += ", " + context.getString(R.string.mintage) + ": " + String.format(Locale.getDefault(), "%,d", mintage);
        if (!series.isEmpty())
            desc += ", " + series;
        return desc;
    }

    public String getMintage() {
        if (mintage > 0)
            return String.format(Locale.getDefault(), "%,d", mintage);
        else
            return "";
    }
    public String getDenomination() {
        return unit;
    }
    public String getYear() {
        if (year > 0)
            return Long.toString(year);
        else
            return "";
    }
    public String getMaterial() { return material; }
    public String getMint() {
        if (!mint.isEmpty()) {
            if (!mintmark.isEmpty())
                return mint + " (" + mintmark + ")";
            else
                return mint;
        }
        if (!mintmark.isEmpty())
            return mintmark;

        return "";
    }
    public String getPrices() {
        String min_price = null, max_price = null;

        if (!price_unc.isEmpty()) {
            max_price = price_unc;
            min_price = price_unc;
        }
        if (!price_xf.isEmpty()) {
            if (max_price == null)
                max_price = price_xf;
            min_price = price_xf;
        }
        if (!price_vf.isEmpty()) {
            if (max_price == null)
                max_price = price_vf;
            min_price = price_vf;
        }
        if (!price_f.isEmpty()) {
            if (max_price == null)
                max_price = price_f;
            min_price = price_f;
        }

        if (min_price == null)
            return "";
        if (!min_price.equals(max_price))
            return min_price + " - " + max_price;
        return min_price;
    }
    public String getCount() {
        return String.format(Locale.getDefault(), "%d", count);
    }
    public String getDate(Context context) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date_obj;
        try {
            date_obj = sdf.parse(date);
        } catch (ParseException e) {
            return date;
        }
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        return dateFormat.format(date_obj);
    }

    public Bitmap getImageBitmap() {
        if (image != null)
            return BitmapFactory.decodeByteArray(image, 0, image.length);
        else
            return Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
    }
    public Bitmap getObverseImageBitmap(int maxSize) {
        if (obverse_image != null) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(obverse_image, 0, obverse_image.length, opts);

            opts.inSampleSize = opts.outWidth / maxSize;
            opts.inJustDecodeBounds = false;
            opts.inScaled = true;
            return BitmapFactory.decodeByteArray(obverse_image, 0, obverse_image.length, opts);
        }
        else
            return Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
    }
    public Bitmap getReverseImageBitmap(int maxSize) {
        if (reverse_image != null) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(reverse_image, 0, reverse_image.length, opts);

            opts.inSampleSize = opts.outWidth / maxSize;
            opts.inJustDecodeBounds = false;
            opts.inScaled = true;
            return BitmapFactory.decodeByteArray(reverse_image, 0, reverse_image.length, opts);
        }
        else
            return Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
    }

    public void addExtra(Cursor cursor) {
        if (cursor.isNull(SUBJECT_EX_COLUMN))
            subject = "";
        else
            subject = cursor.getString(SUBJECT_EX_COLUMN);
        if (cursor.isNull(DATE_EX_COLUMN))
            date = "";
        else
            date = cursor.getString(DATE_EX_COLUMN);
        if (!use_mint || cursor.isNull(MINT_EX_COLUMN))
            mint = "";
        else
            mint = cursor.getString(MINT_EX_COLUMN);
        if (cursor.isNull(MATERIAL_EX_COLUMN))
            material = "";
        else
            material = cursor.getString(MATERIAL_EX_COLUMN);
        obverse_image = cursor.getBlob(OBVERSE_IMAGE_EX_COLUMN);
        reverse_image = cursor.getBlob(REVERSE_IMAGE_EX_COLUMN);
    }

    @Override
    public String toString() {
        return title;
    }
}
