package com.snj.quoteverse.data.database

import com.snj.quoteverse.data.entity.QuoteEntity

/**
 * Curated offline collection of 110 classic, literary, and traditional quotes
 * organized across all ten QuoteVerse categories.
 */
object QuoteSeedData {

    val categories: List<String> = listOf(
        "Motivation",
        "Success",
        "Life",
        "Love",
        "Happiness",
        "Wisdom",
        "Friendship",
        "Attitude",
        "Study",
        "Inspirational"
    )

    fun getInitialQuotes(baseTimestamp: Long = 1_700_000_000_000L): List<QuoteEntity> {
        val rawQuotes = listOf(
            // Motivation (11)
            Triple(
                "Act as if what you do makes a difference. It does.",
                "William James",
                "Motivation"
            ),
            Triple(
                "Begin anywhere; every grand journey starts with one deliberate step.",
                "John Cage",
                "Motivation"
            ),
            Triple(
                "What you get by achieving your goals is not as important as what you become by achieving your goals.",
                "Zig Ziglar",
                "Motivation"
            ),
            Triple(
                "You are never too old to set another goal or to dream a new dream.",
                "C.S. Lewis",
                "Motivation"
            ),
            Triple(
                "Do what you can, with what you have, where you are.",
                "Theodore Roosevelt",
                "Motivation"
            ),
            Triple(
                "Energy and persistence conquer all things.",
                "Benjamin Franklin",
                "Motivation"
            ),
            Triple(
                "Either you run the day, or the day runs you.",
                "Jim Rohn",
                "Motivation"
            ),
            Triple(
                "It always seems impossible until it is done.",
                "Nelson Mandela",
                "Motivation"
            ),
            Triple(
                "Don't watch the clock; do what it does. Keep going.",
                "Sam Levenson",
                "Motivation"
            ),
            Triple(
                "Start where you are. Use what you have. Do what you can.",
                "Arthur Ashe",
                "Motivation"
            ),
            Triple(
                "Small deeds done are better than great deeds planned.",
                "Peter Marshall",
                "Motivation"
            ),

            // Success (11)
            Triple(
                "Success is not final, failure is not fatal: it is the courage to continue that counts.",
                "Winston Churchill",
                "Success"
            ),
            Triple(
                "Opportunities don't happen. You create them.",
                "Chris Grosser",
                "Success"
            ),
            Triple(
                "Try not to become a person of success, but rather try to become a person of value.",
                "Albert Einstein",
                "Success"
            ),
            Triple(
                "The secret of success is constancy to purpose.",
                "Benjamin Disraeli",
                "Success"
            ),
            Triple(
                "Success usually comes to those who are too busy to be looking for it.",
                "Henry David Thoreau",
                "Success"
            ),
            Triple(
                "Patience, persistence, and perspiration make an unbeatable combination for success.",
                "Napoleon Hill",
                "Success"
            ),
            Triple(
                "Before anything else, preparation is the key to success.",
                "Alexander Graham Bell",
                "Success"
            ),
            Triple(
                "The road to success is always under construction.",
                "Lily Tomlin",
                "Success"
            ),
            Triple(
                "Success is liking yourself, liking what you do, and liking how you do it.",
                "Maya Angelou",
                "Success"
            ),
            Triple(
                "I never dreamed about success. I worked for it.",
                "Estee Lauder",
                "Success"
            ),
            Triple(
                "Excellence is not a singular act, but a habit shaped by daily discipline.",
                "Aristotle",
                "Success"
            ),

            // Life (11)
            Triple(
                "In the middle of difficulty lies opportunity.",
                "Albert Einstein",
                "Life"
            ),
            Triple(
                "Life is really simple, but we insist on making it complicated.",
                "Confucius",
                "Life"
            ),
            Triple(
                "Go confidently in the direction of your dreams. Live the life you have imagined.",
                "Henry David Thoreau",
                "Life"
            ),
            Triple(
                "The purpose of life is a life of purpose.",
                "Robert Byrne",
                "Life"
            ),
            Triple(
                "Life isn't about finding yourself. Life is about creating yourself.",
                "George Bernard Shaw",
                "Life"
            ),
            Triple(
                "To live is the rarest thing in the world. Most people exist, that is all.",
                "Oscar Wilde",
                "Life"
            ),
            Triple(
                "Not how long, but how well you have lived is the main thing.",
                "Seneca",
                "Life"
            ),
            Triple(
                "Life shrinks or expands in proportion to one's courage.",
                "Anais Nin",
                "Life"
            ),
            Triple(
                "The unexamined life is not worth living.",
                "Socrates",
                "Life"
            ),
            Triple(
                "Turn your wounds into wisdom and your setbacks into stepping stones.",
                "Oprah Winfrey",
                "Life"
            ),
            Triple(
                "In three words I can sum up everything I've learned about life: it goes on.",
                "Robert Frost",
                "Life"
            ),

            // Love (11)
            Triple(
                "Where there is love there is life.",
                "Mahatma Gandhi",
                "Love"
            ),
            Triple(
                "To love and be loved is to feel the sun from both sides.",
                "David Viscott",
                "Love"
            ),
            Triple(
                "Love is composed of a single soul inhabiting two bodies.",
                "Aristotle",
                "Love"
            ),
            Triple(
                "The best thing to hold onto in life is each other.",
                "Audrey Hepburn",
                "Love"
            ),
            Triple(
                "Keep love in your heart. A life without it is like a sunless garden when the flowers are dead.",
                "Oscar Wilde",
                "Love"
            ),
            Triple(
                "Love recognizes no barriers. It jumps hurdles, leaps fences, and penetrates walls to arrive at its destination full of hope.",
                "Maya Angelou",
                "Love"
            ),
            Triple(
                "Darkness cannot drive out darkness; only light can do that. Hate cannot drive out hate; only love can do that.",
                "Martin Luther King Jr.",
                "Love"
            ),
            Triple(
                "Love cures people—both the ones who give it and the ones who receive it.",
                "Karl Menninger",
                "Love"
            ),
            Triple(
                "We are shaped and fashioned by what we love.",
                "Johann Wolfgang von Goethe",
                "Love"
            ),
            Triple(
                "At the touch of love everyone becomes a poet.",
                "Plato",
                "Love"
            ),
            Triple(
                "A loving heart is the truest wisdom.",
                "Charles Dickens",
                "Love"
            ),

            // Happiness (11)
            Triple(
                "Happiness depends upon ourselves.",
                "Aristotle",
                "Happiness"
            ),
            Triple(
                "For every minute you are angry you lose sixty seconds of happiness.",
                "Ralph Waldo Emerson",
                "Happiness"
            ),
            Triple(
                "Folks are usually about as happy as they make their minds up to be.",
                "Abraham Lincoln",
                "Happiness"
            ),
            Triple(
                "Happiness is not something ready-made. It comes from your own actions.",
                "Dalai Lama",
                "Happiness"
            ),
            Triple(
                "The most wasted of all days is one without laughter.",
                "E.E. Cummings",
                "Happiness"
            ),
            Triple(
                "Very little is needed to make a happy life; it is all within yourself, in your way of thinking.",
                "Marcus Aurelius",
                "Happiness"
            ),
            Triple(
                "Joy is the simplest form of gratitude.",
                "Karl Barth",
                "Happiness"
            ),
            Triple(
                "Happiness is when what you think, what you say, and what you do are in harmony.",
                "Mahatma Gandhi",
                "Happiness"
            ),
            Triple(
                "Count your age by friends, not years. Count your life by smiles, not tears.",
                "John Lennon",
                "Happiness"
            ),
            Triple(
                "The greatest blessings of mankind are within us and within our reach.",
                "Seneca",
                "Happiness"
            ),
            Triple(
                "Enjoy the little things, for one day you may look back and realize they were the big things.",
                "Robert Brault",
                "Happiness"
            ),

            // Wisdom (11)
            Triple(
                "Knowing yourself is the beginning of all wisdom.",
                "Aristotle",
                "Wisdom"
            ),
            Triple(
                "The only true wisdom is in knowing you know nothing.",
                "Socrates",
                "Wisdom"
            ),
            Triple(
                "Silence is a source of great strength.",
                "Lao Tzu",
                "Wisdom"
            ),
            Triple(
                "He who has a why to live can bear almost any how.",
                "Friedrich Nietzsche",
                "Wisdom"
            ),
            Triple(
                "Waste no more time arguing about what a good person should be. Be one.",
                "Marcus Aurelius",
                "Wisdom"
            ),
            Triple(
                "Do not dwell in the past, do not dream of the future, concentrate the mind on the present moment.",
                "Buddha",
                "Wisdom"
            ),
            Triple(
                "It does not matter how slowly you go as long as you do not stop.",
                "Confucius",
                "Wisdom"
            ),
            Triple(
                "Wise people speak because they have something to say; fools because they have to say something.",
                "Plato",
                "Wisdom"
            ),
            Triple(
                "We suffer more often in imagination than in reality.",
                "Seneca",
                "Wisdom"
            ),
            Triple(
                "Knowledge speaks, but wisdom listens.",
                "Jimi Hendrix",
                "Wisdom"
            ),
            Triple(
                "Measure twice, cut once, and never mistake motion for progress.",
                "Traditional Proverb",
                "Wisdom"
            ),

            // Friendship (11)
            Triple(
                "A real friend is one who walks in when the rest of the world walks out.",
                "Walter Winchell",
                "Friendship"
            ),
            Triple(
                "The only way to have a friend is to be one.",
                "Ralph Waldo Emerson",
                "Friendship"
            ),
            Triple(
                "Friendship is born at that moment when one person says to another: What! You too?",
                "C.S. Lewis",
                "Friendship"
            ),
            Triple(
                "Walking with a friend in the dark is better than walking alone in the light.",
                "Helen Keller",
                "Friendship"
            ),
            Triple(
                "True friendship multiplies the good in life and divides its evils.",
                "Baltasar Gracian",
                "Friendship"
            ),
            Triple(
                "A friend is someone who gives you total freedom to be yourself.",
                "Jim Morrison",
                "Friendship"
            ),
            Triple(
                "Kindred spirits are not so scarce as I used to think. It's splendid to find out there are so many of them in the world.",
                "L.M. Montgomery",
                "Friendship"
            ),
            Triple(
                "There is nothing better than a friend, unless it is a friend with chocolate.",
                "Linda Grayson",
                "Friendship"
            ),
            Triple(
                "Each friend represents a world in us, a world possibly not born until they arrive.",
                "Anais Nin",
                "Friendship"
            ),
            Triple(
                "Good friends, good books, and a sleepy conscience: this is the ideal life.",
                "Mark Twain",
                "Friendship"
            ),
            Triple(
                "Friends show their love in times of trouble, not in happiness.",
                "Euripides",
                "Friendship"
            ),

            // Attitude (11)
            Triple(
                "Attitude is a little thing that makes a big difference.",
                "Winston Churchill",
                "Attitude"
            ),
            Triple(
                "Whether you think you can, or you think you can't—you're right.",
                "Henry Ford",
                "Attitude"
            ),
            Triple(
                "You have power over your mind—not outside events. Realize this, and you will find strength.",
                "Marcus Aurelius",
                "Attitude"
            ),
            Triple(
                "A positive attitude causes a chain reaction of positive thoughts, events, and outcomes.",
                "Wade Boggs",
                "Attitude"
            ),
            Triple(
                "Keep your face always toward the sunshine—and shadows will fall behind you.",
                "Walt Whitman",
                "Attitude"
            ),
            Triple(
                "Weakness of attitude becomes weakness of character.",
                "Albert Einstein",
                "Attitude"
            ),
            Triple(
                "Nothing can stop the person with the right mental attitude from achieving their goal.",
                "Thomas Jefferson",
                "Attitude"
            ),
            Triple(
                "Change your thoughts and you change your world.",
                "Norman Vincent Peale",
                "Attitude"
            ),
            Triple(
                "Life is ten percent what happens to you and ninety percent how you react to it.",
                "Charles R. Swindoll",
                "Attitude"
            ),
            Triple(
                "Optimism is the faith that leads to achievement. Nothing can be done without hope and confidence.",
                "Helen Keller",
                "Attitude"
            ),
            Triple(
                "Calmness is the cradle of power.",
                "Josiah Gilbert Holland",
                "Attitude"
            ),

            // Study (11)
            Triple(
                "Live as if you were to die tomorrow. Learn as if you were to live forever.",
                "Mahatma Gandhi",
                "Study"
            ),
            Triple(
                "An investment in knowledge pays the best interest.",
                "Benjamin Franklin",
                "Study"
            ),
            Triple(
                "Tell me and I forget. Teach me and I remember. Involve me and I learn.",
                "Xunzi",
                "Study"
            ),
            Triple(
                "The beautiful thing about learning is that no one can take it away from you.",
                "B.B. King",
                "Study"
            ),
            Triple(
                "Education is the kindling of a flame, not the filling of a vessel.",
                "Socrates",
                "Study"
            ),
            Triple(
                "Develop a passion for learning. If you do, you will never cease to grow.",
                "Anthony J. D'Angelo",
                "Study"
            ),
            Triple(
                "Learning never exhausts the mind.",
                "Leonardo da Vinci",
                "Study"
            ),
            Triple(
                "The expert in anything was once a beginner.",
                "Helen Hayes",
                "Study"
            ),
            Triple(
                "Study the past if you would define the future.",
                "Confucius",
                "Study"
            ),
            Triple(
                "Curiosity is the wick in the candle of learning.",
                "William Arthur Ward",
                "Study"
            ),
            Triple(
                "Discipline is choosing between what you want now and what you want most.",
                "Abraham Lincoln",
                "Study"
            ),

            // Inspirational (11)
            Triple(
                "What lies behind us and what lies before us are tiny matters compared to what lies within us.",
                "Ralph Waldo Emerson",
                "Inspirational"
            ),
            Triple(
                "No one can make you feel inferior without your consent.",
                "Eleanor Roosevelt",
                "Inspirational"
            ),
            Triple(
                "Believe you can and you're halfway there.",
                "Theodore Roosevelt",
                "Inspirational"
            ),
            Triple(
                "The future belongs to those who believe in the beauty of their dreams.",
                "Eleanor Roosevelt",
                "Inspirational"
            ),
            Triple(
                "Be yourself; everyone else is already taken.",
                "Oscar Wilde",
                "Inspirational"
            ),
            Triple(
                "Nothing is impossible; the word itself says 'I'm possible!'",
                "Audrey Hepburn",
                "Inspirational"
            ),
            Triple(
                "Do not go where the path may lead; go instead where there is no path and leave a trail.",
                "Ralph Waldo Emerson",
                "Inspirational"
            ),
            Triple(
                "Every artist was first an amateur.",
                "Ralph Waldo Emerson",
                "Inspirational"
            ),
            Triple(
                "Hold fast to dreams, for if dreams die, life is a broken-winged bird that cannot fly.",
                "Langston Hughes",
                "Inspirational"
            ),
            Triple(
                "Stars can't shine without darkness.",
                "D.H. Sidebottom",
                "Inspirational"
            ),
            Triple(
                "Courage is grace under pressure.",
                "Ernest Hemingway",
                "Inspirational"
            )
        )

        return rawQuotes.mapIndexed { index, (text, author, category) ->
            QuoteEntity(
                quoteText = text,
                author = author,
                category = category,
                isFavorite = false,
                isDailyQuote = index == 0,
                createdAt = baseTimestamp + (index * 1_000L)
            )
        }
    }
}

