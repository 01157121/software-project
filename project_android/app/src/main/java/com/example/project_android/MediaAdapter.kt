import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.project_android.Media
import com.example.project_android.R

class MediaAdapter(private val mediaList: List<Media>) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_feedback, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val media = mediaList[position]
        // 使用 Glide 顯示圖片
        Glide.with(holder.itemView.context)
            .load(media.uri) // 加載圖片 URI
            .into(holder.imageView)
    }

    override fun getItemCount(): Int = mediaList.size

    class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.media_image_view)
    }
}
