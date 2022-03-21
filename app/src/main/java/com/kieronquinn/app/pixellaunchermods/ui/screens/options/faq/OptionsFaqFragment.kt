package com.kieronquinn.app.pixellaunchermods.ui.screens.options.faq

import android.os.Bundle
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.kieronquinn.app.pixellaunchermods.R
import com.kieronquinn.app.pixellaunchermods.databinding.FragmentOptionsFaqBinding
import com.kieronquinn.app.pixellaunchermods.ui.base.BackAvailable
import com.kieronquinn.app.pixellaunchermods.ui.base.BoundFragment
import com.kieronquinn.app.pixellaunchermods.utils.extensions.applyBottomNavigationInset
import com.kieronquinn.app.pixellaunchermods.utils.extensions.getColorResCompat
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.core.MarkwonTheme
import org.commonmark.node.Heading

class OptionsFaqFragment: BoundFragment<FragmentOptionsFaqBinding>(FragmentOptionsFaqBinding::inflate), BackAvailable {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val typeface = ResourcesCompat.getFont(requireContext(), R.font.google_sans_text_medium)
        val monoTypeface = ResourcesCompat.getFont(requireContext(), R.font.google_sans_mono)
        val markwon = Markwon.builder(requireContext()).usePlugin(object: AbstractMarkwonPlugin() {
            override fun configureTheme(builder: MarkwonTheme.Builder) {
                typeface?.let {
                    builder.headingTypeface(it)
                    builder.headingBreakHeight(0)
                }
                monoTypeface?.let {
                    builder.codeBlockTypeface(it)
                }
            }

            override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                val origin = builder.requireFactory(Heading::class.java)
                builder.setFactory(Heading::class.java) { configuration, props ->
                    arrayOf(origin.getSpans(configuration, props), ForegroundColorSpan(requireContext().getColorResCompat(android.R.attr.textColorPrimary)))
                }
            }
        }).build()
        val markdown = requireContext().assets.open("faq.md").bufferedReader().use { it.readText() }
        binding.markdown.text = markwon.toMarkdown(markdown)
        binding.root.applyBottomNavigationInset(resources.getDimension(R.dimen.margin_16))
    }

}