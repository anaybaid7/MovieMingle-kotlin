package com.imn.iicnma.ui.favorites

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.imn.iicnma.R
import com.imn.iicnma.databinding.ListItemFavoritesBinding
import com.imn.iicnma.domain.model.Movie
import com.imn.iicnma.utils.dateTransitionName
import com.imn.iicnma.utils.posterTransitionName
import com.imn.iicnma.utils.titleTransitionName

class FavoritesAdapter(
    private val onItemClick: (Movie, ImageView, TextView, TextView) -> Unit,
) : PagingDataAdapter<Movie, FavoritesItemViewHolder>(MOVIE_COMPARATOR) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesItemViewHolder =
        FavoritesItemViewHolder.create(parent, onItemClick)


    override fun onBindViewHolder(holder: FavoritesItemViewHolder, position: Int) {
        getItem(position)?.let { holder.onBind(it) }
    }

    companion object {
        private val MOVIE_COMPARATOR = object : DiffUtil.ItemCallback<Movie>() {
            override fun areItemsTheSame(old: Movie, new: Movie) = (old.id == new.id)

            override fun areContentsTheSame(old: Movie, new: Movie) = (old == new)
        }
    }
}

class FavoritesItemViewHolder(
    private val binding: ListItemFavoritesBinding,
    private val onItemClick: (Movie, ImageView, TextView, TextView) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    private var _movie: Movie? = null

    init {
        with(binding) {
            root.setOnClickListener {
                _movie?.let {
                    onItemClick.invoke(
                        it,
                        posterImageView,
                        titleTextView,
                        dateTextView
                    )
                }
            }
        }

    }

    fun onBind(movie: Movie) = with(binding) {
        _movie = movie

        titleTextView.text = movie.title
        dateTextView.text = movie.releaseDate
        overviewTextView.text = movie.overview

        posterImageView.transitionName = posterTransitionName(movie.id)
        titleTextView.transitionName = titleTransitionName(movie.id)
        dateTextView.transitionName = dateTransitionName(movie.id)

        Glide.with(root.context)
            .load(movie.posterUrl)
            .placeholder(R.drawable.ic_place_holder_24dp)
            .into(posterImageView)
    }

    companion object {
        fun create(parent: ViewGroup, onItemClick: (Movie, ImageView, TextView, TextView) -> Unit) =
            FavoritesItemViewHolder(
                ListItemFavoritesBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                ),
                onItemClick
            )
    }
}