# -*- coding: utf-8 -*-
class LsecGrammarRuleEngine:
    """Aplica reglas de estructura sintáctica viso-espacial LSEC: Tiempo + Tema + Sujeto + Objeto + Verbo + Negación"""

    TIME_MARKERS = {'ayer', 'hoy', 'manana', 'ahora', 'despues', 'futuro', 'pasado'}
    INTERROGATIVE_MARKERS = {'que', 'quien', 'donde', 'cuando', 'como', 'por que', 'cual'}

    @classmethod
    def reorder_for_lsec(cls, tokens):
        time_tokens = []
        regular_tokens = []
        interrogative_tokens = []
        negation_tokens = []

        for token in tokens:
            t_lower = token.lower()
            if t_lower in cls.TIME_MARKERS:
                time_tokens.append(token)
            elif t_lower in cls.INTERROGATIVE_MARKERS:
                interrogative_tokens.append(token)
            elif t_lower == 'no':
                negation_tokens.append(token)
            else:
                regular_tokens.append(token)

        # Regla: [TIEMPO] + [TÓPICO/ARGUMENTOS] + [NEGACIÓN] + [INTERROGATIVO]
        reordered = time_tokens + regular_tokens + negation_tokens + interrogative_tokens
        return reordered
