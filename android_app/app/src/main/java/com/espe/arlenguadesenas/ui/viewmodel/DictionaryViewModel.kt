package com.espe.arlenguadesenas.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.espe.arlenguadesenas.data.repository.LsecRepository
import com.espe.arlenguadesenas.domain.model.Gloss
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DictionaryViewModel(private val repository: LsecRepository) : ViewModel() {
    private val _glossList = MutableStateFlow<List<Gloss>>(emptyList())
    val glossList: StateFlow<List<Gloss>> = _glossList

    fun search(query: String) {
        viewModelScope.launch {
            _glossList.value = repository.searchGlosses(query)
        }
    }
}
