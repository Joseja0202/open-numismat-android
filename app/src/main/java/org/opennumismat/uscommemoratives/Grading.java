package org.opennumismat.uscommemoratives;

import java.util.Locale;

/**
 * Created by v.ignatov on 20.11.2014.
 */
public class Grading {
    public int grade;
    public String title;
    public String desc;
    public int count;

    public Grading(int grade, String title, String desc) {
        this.grade = grade;
        this.title = title;
        this.desc = desc;
        count = 0;
    }

    public String getCount() {
        return String.format(Locale.getDefault(), "%d", count);
    }
}
