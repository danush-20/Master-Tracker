import android.widget.ScrollView
import android.widget.RemoteViews.RemoteView

fun main() {
    val isRemote = ScrollView::class.java.isAnnotationPresent(RemoteView::class.java)
    println("ScrollView is RemoteView: $isRemote")
}
