package com.imn.iicnma.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import com.imn.iicnma.R
import com.imn.iicnma.databinding.FragmentHomeBinding
import com.imn.iicnma.domain.model.Movie
import com.imn.iicnma.ui.widget.ListLoadStateAdapter
import com.imn.iicnma.utils.isPortrait
import com.imn.iicnma.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding

    private val homeViewModel: HomeViewModel by viewModels()

    private val homeAdapter = HomeAdapter(::onMovieClicked)

    private fun onMovieClicked(
        movie: Movie,
        posterImageView: ImageView,
        titleTextView: TextView,
        dateTextView: TextView,
    ) {
        val extras = FragmentNavigatorExtras(
            posterImageView to posterImageView.transitionName,
            titleTextView to titleTextView.transitionName,
            dateTextView to dateTextView.transitionName,
        )
        findNavController().navigate(
            HomeFragmentDirections.actionNavigationHomeToMovieDetails(movie), extras
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = FragmentHomeBinding.inflate(inflater).also { binding = it; initUI() }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            homeViewModel.movies.collectLatest { homeAdapter.submitData(it) }
        }
    }

    private fun initUI() = with(binding) {
        postponeEnterTransition()

        recyclerView.apply {
            adapter = homeAdapter.withLoadStateHeaderAndFooter(
                header = ListLoadStateAdapter { homeAdapter.retry() },
                footer = ListLoadStateAdapter { homeAdapter.retry() }
            )
            layoutManager = GridLayoutManager(context, getSpansCount()).apply {
                spanSizeLookup = object : SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return if (position >= homeAdapter.itemCount) getSpansCount() else 1
                    }
                }
            }
            viewTreeObserver.addOnPreDrawListener { startPostponedEnterTransition(); true }
        }

        loadStateLayout.retryButton.setOnClickListener { homeAdapter.retry() }
        topMessageTextView.setOnClickListener { homeAdapter.retry() }

        homeAdapter.addLoadStateListener { loadState ->
            recyclerView.isVisible = loadState.refresh is LoadState.NotLoading
            loadStateLayout.progressBar.isVisible = loadState.refresh is LoadState.Loading

            if (loadState.refresh is LoadState.Error) {

                val sourceErrorState = loadState.source.refresh as? LoadState.Error

                if (sourceErrorState != null) {
                    loadStateLayout.retryButton.isVisible = true
                    loadStateLayout.messageTextView.apply {
                        isVisible = true
                        text = sourceErrorState.error.toString()
                    }
                } else {
                    recyclerView.isVisible = true
                    topMessageTextView.isVisible = true
                    loadStateLayout.retryButton.isVisible = false
                    loadStateLayout.messageTextView.isVisible = false
                }

                (loadState.mediator?.refresh as? LoadState.Error)?.let {
                    showToast(getString(R.string.api_error_prefix) + it.error.toString())
                }
            } else {
                topMessageTextView.isVisible = false
                loadStateLayout.retryButton.isVisible = false
                loadStateLayout.messageTextView.isVisible = false
            }
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            private var dySum = 0

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE,
                    RecyclerView.SCROLL_STATE_SETTLING,
                    -> {
                        pageTitle.isVisible = (dySum <= 0)
                        dySum = 0
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                dySum += dy
            }
        })
    }

    private fun getSpansCount() = if (isPortrait()) 2 else 4
}