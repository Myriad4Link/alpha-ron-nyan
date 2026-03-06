package xyz.uthofficial.arnyan.simplified.util

import org.slf4j.LoggerFactory
import java.awt.*
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import javax.swing.*
import javax.swing.border.EmptyBorder
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

class RealTimeGrapher : AutoCloseable {
    private val logger = LoggerFactory.getLogger(RealTimeGrapher::class.java)
    
    private val frame: JFrame
    private val fitnessChartPanel: FitnessChartPanel
    private val sigmaChartPanel: SigmaChartPanel
    private val weightHistogramPanel: WeightHistogramPanel
    private val statsPanel: StatsPanel
    
    private val generationMetrics = mutableListOf<GenerationMetrics>()
    private val offspringEvaluations = mutableListOf<OffspringEvaluation>()
    private var currentGeneration = 0
    private var currentSigma = 0.1f
    private var totalGamesPlayed = 0
    private var totalWins = 0
    private var totalLosses = 0
    private var useSlidingWindow = false
    private val slidingWindowSize = 1000
    private var trainingStartTime = System.currentTimeMillis()
    
    private val lock = Any()
    
    init {
        frame = JFrame("Mahjong AI Training - Real-Time Metrics [Press W to toggle view]")
        frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
        frame.addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(e: java.awt.event.WindowEvent?) {
                frame.isVisible = false
            }
        })
        
        frame.layout = BorderLayout()
        frame.size = Dimension(1400, 900)
        frame.setLocationRelativeTo(null)
        frame.isAlwaysOnTop = false
        
        frame.addKeyListener(object : java.awt.event.KeyAdapter() {
            override fun keyPressed(e: java.awt.event.KeyEvent?) {
                if (e?.keyCode == java.awt.event.KeyEvent.VK_W) {
                    useSlidingWindow = !useSlidingWindow
                    fitnessChartPanel.setSlidingWindow(useSlidingWindow, slidingWindowSize)
                    sigmaChartPanel.setSlidingWindow(useSlidingWindow, slidingWindowSize)
                    frame.title = if (useSlidingWindow) {
                        "Mahjong AI Training - Sliding Window (last $slidingWindowSize gens) [Press W for full view]"
                    } else {
                        "Mahjong AI Training - Full View (all generations) [Press W for sliding window]"
                    }
                    fitnessChartPanel.repaint()
                    sigmaChartPanel.repaint()
                }
            }
        })
        
        val mainPanel = JPanel(GridBagLayout())
        mainPanel.border = EmptyBorder(10, 10, 10, 10)
        
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.BOTH
        gbc.weightx = 0.5
        gbc.weighty = 0.5
        gbc.insets = Insets(5, 5, 5, 5)
        
        fitnessChartPanel = FitnessChartPanel()
        sigmaChartPanel = SigmaChartPanel()
        weightHistogramPanel = WeightHistogramPanel()
        statsPanel = StatsPanel()
        
        gbc.gridx = 0; gbc.gridy = 0
        mainPanel.add(fitnessChartPanel, gbc)
        
        gbc.gridx = 1; gbc.gridy = 0
        mainPanel.add(sigmaChartPanel, gbc)
        
        gbc.gridx = 0; gbc.gridy = 1
        mainPanel.add(weightHistogramPanel, gbc)
        
        gbc.gridx = 1; gbc.gridy = 1
        mainPanel.add(statsPanel, gbc)
        
        frame.add(mainPanel, BorderLayout.CENTER)
        
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        
        val toggleButton = JButton("Toggle View (W)")
        toggleButton.addActionListener { 
            useSlidingWindow = !useSlidingWindow
            fitnessChartPanel.setSlidingWindow(useSlidingWindow, slidingWindowSize)
            sigmaChartPanel.setSlidingWindow(useSlidingWindow, slidingWindowSize)
            frame.title = if (useSlidingWindow) {
                "Mahjong AI Training - Sliding Window (last $slidingWindowSize gens) [Press W for full view]"
            } else {
                "Mahjong AI Training - Full View (all generations) [Press W for sliding window]"
            }
            fitnessChartPanel.repaint()
            sigmaChartPanel.repaint()
        }
        buttonPanel.add(toggleButton)
        
        val closeButton = JButton("Close")
        closeButton.addActionListener { frame.isVisible = false }
        buttonPanel.add(closeButton)
        frame.add(buttonPanel, BorderLayout.SOUTH)
        
        frame.isVisible = true
        
        thread {
            Thread.sleep(100)
            frame.toFront()
            frame.requestFocusInWindow()
            frame.repaint()
        }
        
        trainingStartTime = System.currentTimeMillis()
        logger.info("Real-time grapher initialized")
    }
    
    fun update(update: MetricUpdate) {
        SwingUtilities.invokeLater {
            when (update) {
                is MetricUpdate.OffspringEvaluated -> handleOffspringEvaluated(update.evaluation)
                is MetricUpdate.GenerationComplete -> handleGenerationComplete(update.metrics)
                is MetricUpdate.WeightsUpdated -> handleWeightsUpdated(update.stats)
                is MetricUpdate.TrainingComplete -> handleTrainingComplete()
            }
        }
    }
    
    private fun handleOffspringEvaluated(evaluation: OffspringEvaluation) {
        synchronized(lock) {
            offspringEvaluations.add(evaluation)
            totalGamesPlayed += evaluation.gamesPlayed
            currentGeneration = evaluation.generation
        }
        fitnessChartPanel.updateData(generationMetrics.toList(), offspringEvaluations.toList())
        fitnessChartPanel.repaint()
        statsPanel.updateStats(totalGamesPlayed, totalWins, totalLosses, currentGeneration, evaluation.fitness)
        statsPanel.repaint()
    }
    
    private fun handleGenerationComplete(metrics: GenerationMetrics) {
        synchronized(lock) {
            generationMetrics.add(metrics)
            currentGeneration = metrics.generation
            currentSigma = metrics.mutationSigma
        }
        fitnessChartPanel.updateData(generationMetrics.toList(), offspringEvaluations.toList())
        fitnessChartPanel.setSlidingWindow(useSlidingWindow, slidingWindowSize)
        fitnessChartPanel.repaint()
        sigmaChartPanel.updateData(generationMetrics.toList())
        sigmaChartPanel.setSlidingWindow(useSlidingWindow, slidingWindowSize)
        sigmaChartPanel.repaint()
        
        val elapsedSeconds = (System.currentTimeMillis() - trainingStartTime) / 1000.0
        val gensPerSecond = if (elapsedSeconds > 0) generationMetrics.size / elapsedSeconds else 0.0
        val remainingGens = currentGeneration.coerceAtLeast(1).let { maxGenerations -> maxGenerations - currentGeneration }
        val etaSeconds = if (gensPerSecond > 0) remainingGens / gensPerSecond else 0.0
        
        statsPanel.updateStats(totalGamesPlayed, totalWins, totalLosses, currentGeneration, metrics.parentFitness)
        statsPanel.updateGenerationStats(
            metrics.generation,
            metrics.parentFitness,
            metrics.bestOffspringFitness,
            metrics.avgOffspringFitness,
            metrics.fitnessImprovement,
            gensPerSecond,
            etaSeconds
        )
        statsPanel.repaint()
    }
    
    private fun handleWeightsUpdated(stats: WeightStatistics) {
        weightHistogramPanel.updateStats(stats)
        weightHistogramPanel.repaint()
    }
    
    private fun handleTrainingComplete() {
        logger.info("Training complete - ${generationMetrics.size} generations processed")
        JOptionPane.showMessageDialog(
            frame,
            "Training Complete!\n" +
            "Generations: ${generationMetrics.size}\n" +
            "Best Fitness: ${generationMetrics.maxOfOrNull { it.bestOffspringFitness } ?: 0f}\n" +
            "Total Games: $totalGamesPlayed",
            "Training Finished",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    
    override fun close() {
        frame.dispose()
    }
    
    private class FitnessChartPanel : JPanel() {
        private var metrics: List<GenerationMetrics> = emptyList()
        private var offspringEvals: List<OffspringEvaluation> = emptyList()
        private var useSlidingWindow = false
        private var slidingWindowSize = 1000
        
        init {
            preferredSize = Dimension(600, 400)
            minimumSize = Dimension(300, 200)
            background = Color.WHITE
        }
        
        fun updateData(m: List<GenerationMetrics>, o: List<OffspringEvaluation>) {
            metrics = m
            offspringEvals = o
        }
        
        fun setSlidingWindow(enabled: Boolean, size: Int) {
            useSlidingWindow = enabled
            slidingWindowSize = size
        }
        
        private fun getVisibleMetrics(): List<GenerationMetrics> {
            if (!useSlidingWindow || metrics.isEmpty()) return metrics
            val maxGen = metrics.maxOfOrNull { it.generation } ?: 0
            val minGen = (maxGen - slidingWindowSize + 1).coerceAtLeast(0)
            return metrics.filter { it.generation >= minGen }
        }
        
        private fun getVisibleOffspring(): List<OffspringEvaluation> {
            if (!useSlidingWindow || offspringEvals.isEmpty()) return offspringEvals
            val maxGen = offspringEvals.maxOfOrNull { it.generation } ?: 0
            val minGen = (maxGen - slidingWindowSize + 1).coerceAtLeast(0)
            return offspringEvals.filter { it.generation >= minGen }
        }
        
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            
            g2.color = Color.WHITE
            g2.fill(Rectangle2D.Double(0.0, 0.0, width.toDouble(), height.toDouble()))
            
            val visibleMetrics = getVisibleMetrics()
            val visibleOffspring = getVisibleOffspring()
            
            if (visibleMetrics.isEmpty() && visibleOffspring.isEmpty()) {
                g2.color = Color.GRAY
                g2.font = Font("Arial", Font.PLAIN, 14)
                g2.drawString("Waiting for data...", 20, 30)
                return
            }
            
            val padding = 60
            val chartWidth = width - 2 * padding
            val chartHeight = height - 2 * padding
            
            val allFitnessValues = mutableListOf<Float>()
            visibleMetrics.forEach { m ->
                allFitnessValues.add(m.parentFitness)
                allFitnessValues.add(m.bestOffspringFitness)
                allFitnessValues.add(m.avgOffspringFitness)
            }
            visibleOffspring.forEach { allFitnessValues.add(it.fitness) }
            
            if (allFitnessValues.isEmpty()) return
            
            val minFitness = min(0f, allFitnessValues.minOrNull() ?: 0f)
            val maxFitness = max(50f, allFitnessValues.maxOrNull() ?: 50f)
            val fitnessRange = maxFitness - minFitness
            
            val maxGen = max(1, max(visibleMetrics.maxOfOrNull { it.generation } ?: 0, visibleOffspring.maxOfOrNull { it.generation } ?: 0))
            val minGen = if (useSlidingWindow) visibleMetrics.minOfOrNull { it.generation } ?: 0 else 0
            
            g2.color = Color.BLACK
            g2.font = Font("Arial", Font.BOLD, 16)
            val title = if (useSlidingWindow) "Fitness (Gen ${minGen}-${maxGen})" else "Fitness Over Generations"
            g2.drawString(title, width / 2 - 100, 25)
            
            g2.color = Color.LIGHT_GRAY
            g2.draw(Rectangle2D.Double(padding.toDouble(), padding.toDouble(), chartWidth.toDouble(), chartHeight.toDouble()))
            
            for (i in 0..5) {
                val y = padding + (chartHeight * i / 5)
                val fitness = maxFitness - (fitnessRange * i / 5)
                g2.color = Color.LIGHT_GRAY
                g2.draw(Line2D.Double(padding.toDouble(), y.toDouble(), (padding + chartWidth).toDouble(), y.toDouble()))
                g2.color = Color.GRAY
                g2.font = Font("Arial", Font.PLAIN, 10)
                g2.drawString("%.1f".format(fitness), 5, y + 4)
            }
            
            val xScaleFactor = if (useSlidingWindow && minGen > 0) {
                { gen: Int -> padding + (chartWidth * (gen - minGen) / (maxGen - minGen).coerceAtLeast(1)) }
            } else {
                { gen: Int -> padding + (chartWidth * gen / maxGen) }
            }
            
            for (i in 0..5) {
                val y = padding + (chartHeight * i / 5)
                val fitness = maxFitness - (fitnessRange * i / 5)
                g2.color = Color.LIGHT_GRAY
                g2.draw(Line2D.Double(padding.toDouble(), y.toDouble(), (padding + chartWidth).toDouble(), y.toDouble()))
                g2.color = Color.GRAY
                g2.font = Font("Arial", Font.PLAIN, 10)
                g2.drawString("%.1f".format(fitness), 5, y + 4)
            }
            
            val xStep = if (useSlidingWindow) max(1, (maxGen - minGen) / 10) else max(1, maxGen / 10)
            for (gen in (if (useSlidingWindow) minGen else 0)..maxGen step xStep) {
                val x = xScaleFactor(gen)
                g2.color = Color.LIGHT_GRAY
                g2.draw(Line2D.Double(x.toDouble(), padding.toDouble(), x.toDouble(), (padding + chartHeight).toDouble()))
                g2.color = Color.GRAY
                g2.font = Font("Arial", Font.PLAIN, 10)
                g2.drawString("$gen", x - 5, height - 5)
            }
            
            if (visibleMetrics.isNotEmpty()) {
                val parentPoints = visibleMetrics.map { m ->
                    val x = xScaleFactor(m.generation)
                    val y = padding + (chartHeight * (1 - (m.parentFitness - minFitness) / fitnessRange))
                    Point(x, y.toInt())
                }
                
                g2.color = Color.BLUE
                g2.stroke = BasicStroke(2f)
                for (i in 0 until parentPoints.size - 1) {
                    g2.draw(Line2D.Double(
                        parentPoints[i].x.toDouble(), parentPoints[i].y.toDouble(),
                        parentPoints[i + 1].x.toDouble(), parentPoints[i + 1].y.toDouble()
                    ))
                }
                parentPoints.forEach { p ->
                    g2.fillOval(p.x - 3, p.y - 3, 6, 6)
                }
                
                val bestPoints = visibleMetrics.map { m ->
                    val x = xScaleFactor(m.generation)
                    val y = padding + (chartHeight * (1 - (m.bestOffspringFitness - minFitness) / fitnessRange))
                    Point(x, y.toInt())
                }
                
                g2.color = Color.GREEN
                for (i in 0 until bestPoints.size - 1) {
                    g2.draw(Line2D.Double(
                        bestPoints[i].x.toDouble(), bestPoints[i].y.toDouble(),
                        bestPoints[i + 1].x.toDouble(), bestPoints[i + 1].y.toDouble()
                    ))
                }
                bestPoints.forEach { p ->
                    g2.fillOval(p.x - 3, p.y - 3, 6, 6)
                }
                
                val avgPoints = visibleMetrics.map { m ->
                    val x = xScaleFactor(m.generation)
                    val y = padding + (chartHeight * (1 - (m.avgOffspringFitness - minFitness) / fitnessRange))
                    Point(x, y.toInt())
                }
                
                g2.color = Color.CYAN
                g2.stroke = BasicStroke(1f)
                for (i in 0 until avgPoints.size - 1) {
                    g2.draw(Line2D.Double(
                        avgPoints[i].x.toDouble(), avgPoints[i].y.toDouble(),
                        avgPoints[i + 1].x.toDouble(), avgPoints[i + 1].y.toDouble()
                    ))
                }
            }
            
            val currentOffspring = visibleOffspring.filter { it.generation == maxGen }
            if (currentOffspring.isNotEmpty()) {
                g2.color = Color.ORANGE
                currentOffspring.forEach { eval ->
                    val x = xScaleFactor(eval.generation) + (eval.offspringIndex * 5)
                    val y = padding + (chartHeight * (1 - (eval.fitness - minFitness) / fitnessRange))
                    g2.fillRect(x, y.toInt(), 4, 4)
                }
            }
            
            g2.color = Color.BLACK
            g2.font = Font("Arial", Font.PLAIN, 11)
            g2.stroke = BasicStroke(1f)
            
            var legendX = padding
            val legendY = 15
            
            g2.color = Color.BLUE
            g2.drawLine(legendX, legendY - 4, legendX + 20, legendY - 4)
            g2.color = Color.BLACK
            g2.drawString("Parent", legendX + 25, legendY)
            legendX += 70
            
            g2.color = Color.GREEN
            g2.drawLine(legendX, legendY - 4, legendX + 20, legendY - 4)
            g2.color = Color.BLACK
            g2.drawString("Best", legendX + 25, legendY)
            legendX += 60
            
            g2.color = Color.CYAN
            g2.drawLine(legendX, legendY - 4, legendX + 20, legendY - 4)
            g2.color = Color.BLACK
            g2.drawString("Avg", legendX + 25, legendY)
            legendX += 55
            
            g2.color = Color.ORANGE
            g2.fillRect(legendX, legendY - 6, 8, 8)
            g2.color = Color.BLACK
            g2.drawString("Current", legendX + 12, legendY)
            
            g2.drawString("Generation", width / 2 - 30, height - 20)
        }
    }
    
    private class SigmaChartPanel : JPanel() {
        private var metrics: List<GenerationMetrics> = emptyList()
        private var useSlidingWindow = false
        private var slidingWindowSize = 1000
        
        init {
            preferredSize = Dimension(600, 400)
            minimumSize = Dimension(300, 200)
            background = Color.WHITE
        }
        
        fun updateData(m: List<GenerationMetrics>) {
            metrics = m
        }
        
        fun setSlidingWindow(enabled: Boolean, size: Int) {
            useSlidingWindow = enabled
            slidingWindowSize = size
        }
        
        private fun getVisibleMetrics(): List<GenerationMetrics> {
            if (!useSlidingWindow || metrics.isEmpty()) return metrics
            val maxGen = metrics.maxOfOrNull { it.generation } ?: 0
            val minGen = (maxGen - slidingWindowSize + 1).coerceAtLeast(0)
            return metrics.filter { it.generation >= minGen }
        }
        
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            
            g2.color = Color.WHITE
            g2.fill(Rectangle2D.Double(0.0, 0.0, width.toDouble(), height.toDouble()))
            
            val visibleMetrics = getVisibleMetrics()
            
            if (visibleMetrics.isEmpty()) {
                g2.color = Color.GRAY
                g2.font = Font("Arial", Font.PLAIN, 14)
                g2.drawString("Waiting for data...", 20, 30)
                return
            }
            
            val padding = 60
            val chartWidth = width - 2 * padding
            val chartHeight = height - 2 * padding
            
            val maxGen = visibleMetrics.maxOfOrNull { it.generation } ?: 1
            val minGen = if (useSlidingWindow) visibleMetrics.minOfOrNull { it.generation } ?: 0 else 0
            val maxSigma = max(0.15f, visibleMetrics.maxOfOrNull { it.mutationSigma } ?: 0.15f)
            val minSigma = 0f
            
            g2.color = Color.BLACK
            g2.font = Font("Arial", Font.BOLD, 16)
            val title = if (useSlidingWindow) "Sigma Decay (Gen ${minGen}-${maxGen})" else "Mutation Sigma Decay"
            g2.drawString(title, width / 2 - 80, 25)
            
            g2.color = Color.LIGHT_GRAY
            g2.draw(Rectangle2D.Double(padding.toDouble(), padding.toDouble(), chartWidth.toDouble(), chartHeight.toDouble()))
            
            for (i in 0..5) {
                val y = padding + (chartHeight * i / 5)
                val sigma = maxSigma - (maxSigma * i / 5)
                g2.color = Color.LIGHT_GRAY
                g2.draw(Line2D.Double(padding.toDouble(), y.toDouble(), (padding + chartWidth).toDouble(), y.toDouble()))
                g2.color = Color.GRAY
                g2.font = Font("Arial", Font.PLAIN, 10)
                g2.drawString("%.3f".format(sigma), 5, y + 4)
            }
            
            val xScaleFactor = if (useSlidingWindow && minGen > 0) {
                { gen: Int -> padding + (chartWidth * (gen - minGen) / (maxGen - minGen).coerceAtLeast(1)) }
            } else {
                { gen: Int -> padding + (chartWidth * gen / maxGen) }
            }
            
            for (i in 0..5) {
                val y = padding + (chartHeight * i / 5)
                val sigma = maxSigma - (maxSigma * i / 5)
                g2.color = Color.LIGHT_GRAY
                g2.draw(Line2D.Double(padding.toDouble(), y.toDouble(), (padding + chartWidth).toDouble(), y.toDouble()))
                g2.color = Color.GRAY
                g2.font = Font("Arial", Font.PLAIN, 10)
                g2.drawString("%.3f".format(sigma), 5, y + 4)
            }
            
            val xStep = if (useSlidingWindow) max(1, (maxGen - minGen) / 10) else max(1, maxGen / 10)
            for (gen in (if (useSlidingWindow) minGen else 0)..maxGen step xStep) {
                val x = xScaleFactor(gen)
                g2.color = Color.LIGHT_GRAY
                g2.draw(Line2D.Double(x.toDouble(), padding.toDouble(), x.toDouble(), (padding + chartHeight).toDouble()))
                g2.color = Color.GRAY
                g2.font = Font("Arial", Font.PLAIN, 10)
                g2.drawString("$gen", x - 5, height - 5)
            }
            
            val sigmaPoints = visibleMetrics.map { m ->
                val x = xScaleFactor(m.generation)
                val y = padding + (chartHeight * (1 - (m.mutationSigma - minSigma) / maxSigma))
                Point(x, y.toInt())
            }
            
            g2.color = Color.RED
            g2.stroke = BasicStroke(2f)
            for (i in 0 until sigmaPoints.size - 1) {
                g2.draw(Line2D.Double(
                    sigmaPoints[i].x.toDouble(), sigmaPoints[i].y.toDouble(),
                    sigmaPoints[i + 1].x.toDouble(), sigmaPoints[i + 1].y.toDouble()
                ))
            }
            sigmaPoints.forEach { p ->
                g2.fillOval(p.x - 3, p.y - 3, 6, 6)
            }
            
            g2.color = Color.BLACK
            g2.font = Font("Arial", Font.PLAIN, 11)
            g2.drawString("Generation", width / 2 - 30, height - 20)
        }
    }
    
    private class WeightHistogramPanel : JPanel() {
        private var currentStats: WeightStatistics? = null
        
        init {
            preferredSize = Dimension(600, 400)
            minimumSize = Dimension(300, 200)
            background = Color.WHITE
        }
        
        fun updateStats(stats: WeightStatistics) {
            currentStats = stats
        }
        
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            
            g2.color = Color.WHITE
            g2.fill(Rectangle2D.Double(0.0, 0.0, width.toDouble(), height.toDouble()))
            
            if (currentStats == null) {
                g2.color = Color.GRAY
                g2.font = Font("Arial", Font.PLAIN, 14)
                g2.drawString("Waiting for weight data...", 20, 30)
                return
            }
            
            val stats = currentStats!!
            val padding = 60
            val chartWidth = width - 2 * padding
            val chartHeight = height - 2 * padding
            
            g2.color = Color.BLACK
            g2.font = Font("Arial", Font.BOLD, 16)
            g2.drawString("Weight Distribution (Gen ${stats.generation})", width / 2 - 120, 25)
            
            g2.color = Color.LIGHT_GRAY
            g2.draw(Rectangle2D.Double(padding.toDouble(), padding.toDouble(), chartWidth.toDouble(), chartHeight.toDouble()))
            
            if (stats.histogram.isEmpty()) return
            
            val maxCount = stats.histogram.maxOrNull() ?: 1
            val binWidth = chartWidth.toFloat() / stats.histogram.size
            
            g2.color = Color.BLUE
            for (i in stats.histogram.indices) {
                val binHeight = (stats.histogram[i].toFloat() / maxCount * chartHeight).toInt()
                val x = padding + (i * binWidth).toInt()
                val y = padding + chartHeight - binHeight
                g2.fillRect(x, y, (binWidth - 2).toInt().coerceAtLeast(1), binHeight)
            }
            
            g2.color = Color.BLACK
            g2.font = Font("Arial", Font.PLAIN, 10)
            g2.drawString("Min: %.3f".format(stats.min), padding, height - 35)
            g2.drawString("Max: %.3f".format(stats.max), width / 2 - 40, height - 35)
            g2.drawString("Mean: %.3f".format(stats.mean), width / 2 + 20, height - 35)
            g2.drawString("StdDev: %.3f".format(stats.stdDev), width - 100, height - 35)
        }
    }
    
    private class StatsPanel : JPanel() {
        private var totalGames = 0
        private var totalWins = 0
        private var totalLosses = 0
        private var currentGen = 0
        private var currentFitness = 0f
        private var parentFitness = 0f
        private var bestOffspringFitness = 0f
        private var avgOffspringFitness = 0f
        private var fitnessImprovement = 0f
        private var gensPerSecond = 0.0
        private var etaSeconds = 0.0
        
        init {
            preferredSize = Dimension(600, 400)
            minimumSize = Dimension(300, 200)
            layout = null
            background = Color.WHITE
        }
        
        fun updateStats(games: Int, wins: Int, losses: Int, gen: Int, fitness: Float) {
            totalGames = games
            totalWins = wins
            totalLosses = losses
            currentGen = gen
            currentFitness = fitness
        }
        
        fun updateGenerationStats(gen: Int, parent: Float, best: Float, avg: Float, improvement: Float, gps: Double = 0.0, eta: Double = 0.0) {
            parentFitness = parent
            bestOffspringFitness = best
            avgOffspringFitness = avg
            fitnessImprovement = improvement
            gensPerSecond = gps
            etaSeconds = eta
        }
        
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            
            g2.color = Color.WHITE
            g2.fill(Rectangle2D.Double(0.0, 0.0, width.toDouble(), height.toDouble()))
            
            g2.color = Color.BLACK
            g2.font = Font("Arial", Font.BOLD, 16)
            g2.drawString("Training Statistics", 20, 30)
            
            g2.font = Font("Arial", Font.PLAIN, 13)
            var y = 60
            val lineHeight = 25
            val x = 20
            
            g2.drawString("Current Generation: $currentGen", x, y); y += lineHeight
            g2.drawString("Speed: %.2f gen/sec".format(gensPerSecond), x, y); y += lineHeight
            if (etaSeconds > 0) {
                val etaHours = etaSeconds / 3600
                val etaDays = etaHours / 24
                val etaText = if (etaDays >= 1) {
                    "%.1f days".format(etaDays)
                } else if (etaHours >= 1) {
                    "%.1f hours".format(etaHours)
                } else {
                    "%.0f min".format(etaSeconds / 60)
                }
                g2.drawString("ETA: $etaText", x, y); y += lineHeight
            }
            g2.drawString("Total Games Played: $totalGames", x, y); y += lineHeight
            g2.drawString("Current Fitness: ${currentFitness.format(2)}", x, y); y += lineHeight * 2
            
            g2.font = Font("Arial", Font.BOLD, 14)
            g2.drawString("Latest Generation Metrics:", x, y); y += lineHeight
            g2.font = Font("Arial", Font.PLAIN, 13)
            g2.drawString("  Parent Fitness: ${parentFitness.format(2)}", x, y); y += lineHeight
            g2.drawString("  Best Offspring: ${bestOffspringFitness.format(2)}", x, y); y += lineHeight
            g2.drawString("  Avg Offspring: ${avgOffspringFitness.format(2)}", x, y); y += lineHeight
            g2.drawString("  Improvement: ${fitnessImprovement.format(2)}", x, y); y += lineHeight
            
            val winRate = if (totalGames > 0) totalWins * 100.0 / totalGames else 0.0
            val barWidth = 200
            val barHeight = 20
            y += lineHeight
            g2.font = Font("Arial", Font.BOLD, 13)
            g2.drawString("Win Rate:", x, y); y += lineHeight + 5
            
            g2.color = Color.LIGHT_GRAY
            g2.fillRect(x, y, barWidth, barHeight)
            g2.color = Color.GREEN
            val fillWidth = (barWidth * winRate / 100).toInt().coerceAtLeast(0)
            if (fillWidth > 0) {
                g2.fillRect(x, y, fillWidth, barHeight)
            }
            g2.color = Color.BLACK
            g2.drawRect(x, y, barWidth, barHeight)
            g2.drawString("%.1f%%".format(winRate), x + barWidth + 10, y + 15)
        }
    }
}

private fun Float.format(decimalPlaces: Int): String {
    return "%.${decimalPlaces}f".format(this)
}
