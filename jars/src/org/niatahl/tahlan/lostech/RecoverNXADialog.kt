package org.niatahl.tahlan.lostech

import org.niatahl.tahlan.questgiver.IInteractionLogic
import org.niatahl.tahlan.questgiver.InteractionDialogLogic
import org.niatahl.tahlan.questgiver.Questgiver.game
import org.niatahl.tahlan.questgiver.wispLib.findFirst

/**
 * Chase after the ship, finding it in a nearby system, guarded by a much larger and stronger fleet of defenders.
 * Defeat fleet and recover the NX-A.
 * Cieve obtains whatever she was looking for from the ship’s database,
 * reminisces of her past involvement with its construction and thanks the player.
 * “It’s yours now”
 */
class RecoverNXADialog(
        mission: NXAHubMission = game.sector.intelManager.findFirst()!!
) : InteractionDialogLogic<RecoverNXADialog>(
        onInteractionStarted = null,
        people = { listOfNotNull(NXAHubMission.cieve) },
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

                                            mission.setCurrentStage(NXAHubMission.Stage.AwaitAllmotherOffer, dialog, null)
                                            navigator.promptToContinue(game.text["nxa_stg1_pg1_opt1_onSelected_continue"]) {
                                                it.close(doNotOfferAgain = true)
                                            }
                                        }
                                ),
                                IInteractionLogic.Option(
                                        // put the dialog text in a comment for reference, makes file easier to read
                                        text = { game.text["nxa_stg1_pg1_opt2"] },
                                        onOptionSelected = { navigator ->
                                            para { game.text["nxa_stg1_pg1_opt2_onSelected"] }
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