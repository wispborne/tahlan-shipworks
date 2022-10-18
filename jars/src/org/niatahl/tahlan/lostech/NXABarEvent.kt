package org.niatahl.tahlan.lostech

import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.questgiver.BarEventLogic
import org.niatahl.tahlan.questgiver.IInteractionLogic
import org.niatahl.tahlan.questgiver.Questgiver.game

class NXABarEvent : BarEventLogic<NXAHubMission>(
    createInteractionPrompt = {
        para { game.text["nxa_stg1_prompt"] }
    },
    textToStartInteraction = {
        Option(
            text = game.text["nxa_stg1_startBarEvent"],
            textColor = Misc.getHighlightColor()
        )
    },
    onInteractionStarted = {},
    pages = listOf(
        IInteractionLogic.Page(
            id = 1,
            onPageShown = {
                para { game.text["nxa_stg1_pg1_para1"] }
                para { game.text["nxa_stg1_pg1_para2"] }
                para { game.text["nxa_stg1_pg1_para3"] }
            },
            options = listOf(
                IInteractionLogic.Option(
                    // accept
                    text = { game.text["nxa_stg1_pg1_opt1"] },
                    onOptionSelected = {
                        para { game.text["nxa_stg1_pg1_opt1_onSelected"] }
                        mission.setCurrentStage(NXAHubMission.Stage.GoToScanMissionDest, dialog, null)
                        navigator.promptToContinue(game.text["nxa_stg1_pg1_opt1_onSelected_continue"]) {
                            it.close(doNotOfferAgain = true)
                        }
                    }
                ),
                IInteractionLogic.Option(
                    // put the dialog text in a comment for reference, makes file easier to read
                    showIf = { NXAHubMission.choices.askedWhyNotBuyOwnShip != true },
                    text = { game.text["nxa_stg1_pg1_opt2"] },
                    onOptionSelected = { navigator ->
                        para { game.text["nxa_stg1_pg1_opt2_onSelected"] }
                        NXAHubMission.choices.askedWhyNotBuyOwnShip = true
                        navigator.refreshOptions()
                    }
                ),
                IInteractionLogic.Option(
                    // decline
                    text = { game.text["nxa_stg1_pg1_opt3"] },
                    onOptionSelected = { navigator ->
                        para { game.text["nxa_stg1_pg1_opt3_onSelected"] }
                        navigator.promptToContinue(game.text["continue"]) {
                            navigator.close(doNotOfferAgain = true)
                        }
                    }
                )
            )
        )
    )
)
