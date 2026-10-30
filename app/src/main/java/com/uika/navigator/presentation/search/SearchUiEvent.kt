package com.uika.navigator.presentation.search

sealed class SearchUiEvent{
    object SearchSuccess: SearchUiEvent()
    object SearchInvalid: SearchUiEvent()
}
