package com.example.eyesbuddy.domain.quotes

/** A short companion message that can be displayed and optionally spoken. */
data class Quote(val text: String, val category: QuoteCategory)

/** The gentle reminder area covered by a quote. */
enum class QuoteCategory { BREAK, HYDRATION, POSTURE, EYE_REST, PERSPECTIVE }

/** Supplies quotes independently of their storage or network source. */
interface QuoteRepository {
    suspend fun quotes(): List<Quote>
}

/** Local quote source used offline and suitable for later replacement by a remote source. */
class LocalQuoteRepository : QuoteRepository {
    override suspend fun quotes(): List<Quote> = LOCAL_QUOTES

    private companion object {
        val LOCAL_QUOTES = listOf(
            Quote("Take a tiny break. Your eyes have earned a plot twist.", QuoteCategory.BREAK),
            Quote("No rush. The important thing will still be important in five minutes.", QuoteCategory.PERSPECTIVE),
            Quote("Unclench your shoulders. They are not earrings.", QuoteCategory.POSTURE),
            Quote("A sip of water for the human behind the screen.", QuoteCategory.HYDRATION),
            Quote("Look at something far away for a little while. I will keep watch.", QuoteCategory.EYE_REST),
            Quote("You are allowed to do one thing at a time. Revolutionary, I know.", QuoteCategory.PERSPECTIVE),
            Quote("Blink slowly. We are going for relaxed, not startled squirrel energy.", QuoteCategory.EYE_REST),
            Quote("Stretch your hands. Tiny keyboard yoga counts.", QuoteCategory.POSTURE),
            Quote("Drink some water before your brain files a complaint.", QuoteCategory.HYDRATION),
            Quote("Step away for a minute. The screen is not going anywhere.", QuoteCategory.BREAK),
            Quote("Good enough is having a lovely day today.", QuoteCategory.PERSPECTIVE),
            Quote("Let your jaw hang out. It has been working hard too.", QuoteCategory.POSTURE),
            Quote("Rest your focus on the room, not just the rectangle.", QuoteCategory.EYE_REST),
            Quote("A pause is part of the work, not an admission of defeat.", QuoteCategory.BREAK),
            Quote("Water break? I am making an extremely subtle suggestion.", QuoteCategory.HYDRATION),
            Quote("Lower the shoulders. Let gravity be useful for once.", QuoteCategory.POSTURE),
            Quote("Your to-do list can wait while you blink at the horizon.", QuoteCategory.EYE_REST),
            Quote("You do not have to win the whole day before lunch.", QuoteCategory.PERSPECTIVE),
            Quote("Stand up and give your chair a moment to miss you.", QuoteCategory.BREAK),
            Quote("Hydration: the least dramatic upgrade with excellent results.", QuoteCategory.HYDRATION),
            Quote("Soft eyes, easy breath, no gold medal for rushing.", QuoteCategory.EYE_REST),
            Quote("Roll your neck gently. It is attached to a whole person, remember.", QuoteCategory.POSTURE),
            Quote("One calm minute is still a productive minute.", QuoteCategory.PERSPECTIVE),
            Quote("The tab can stay open while you take care of the person opening it.", QuoteCategory.BREAK),
            Quote("Sip first, solve second.", QuoteCategory.HYDRATION),
            Quote("Give your eyes a view with more depth than a flat screen.", QuoteCategory.EYE_REST),
            Quote("Drop the perfectionism one notch. It looks better on you.", QuoteCategory.PERSPECTIVE),
            Quote("Feet on the floor, shoulders down, champion posture.", QuoteCategory.POSTURE),
            Quote("You have permission to pause without writing a report about it.", QuoteCategory.BREAK),
            Quote("A glass of water is a very small favor to future-you.", QuoteCategory.HYDRATION),
            Quote("Blink like you mean it. Your eyes are not decorative buttons.", QuoteCategory.EYE_REST),
            Quote("Make room for a slow stretch between fast thoughts.", QuoteCategory.POSTURE),
            Quote("Progress is allowed to be quiet today.", QuoteCategory.PERSPECTIVE),
            Quote("Close the loop by taking a lap around the room.", QuoteCategory.BREAK),
            Quote("Refill the water, then come back when you are ready.", QuoteCategory.HYDRATION),
            Quote("Distance makes the eyes happy and the mind a little less zoomed in.", QuoteCategory.EYE_REST),
            Quote("You can care deeply without gripping everything tightly.", QuoteCategory.PERSPECTIVE),
            Quote("Uncurl your fingers. The keyboard is not escaping.", QuoteCategory.POSTURE),
            Quote("A quiet reset beats a heroic crash later.", QuoteCategory.BREAK),
            Quote("Take the pressure down to a comfortable simmer.", QuoteCategory.PERSPECTIVE)
        )
    }
}

/** Chooses a new quote only after the configured interval has elapsed. */
class QuoteScheduler(private val intervalMillis: Long = DEFAULT_INTERVAL_MILLIS) {
    private var lastShownAt = Long.MIN_VALUE
    private var nextIndex = 0

    fun nextIfDue(nowMillis: Long, quotes: List<Quote>): Quote? {
        if (quotes.isEmpty() || nowMillis - lastShownAt < intervalMillis) return null
        val quote = quotes[nextIndex % quotes.size]
        nextIndex += 1
        lastShownAt = nowMillis
        return quote
    }

    fun reset(nowMillis: Long = Long.MIN_VALUE) {
        lastShownAt = nowMillis
        nextIndex = 0
    }

    private companion object {
        const val DEFAULT_INTERVAL_MILLIS = 35 * 60 * 1000L
    }
}
