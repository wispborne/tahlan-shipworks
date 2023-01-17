package org.niatahl.tahlan.lostech

import org.niatahl.tahlan.questgiver.IInteractionLogic
import org.niatahl.tahlan.questgiver.InteractionDialogLogic
import org.niatahl.tahlan.questgiver.Questgiver.game
import org.niatahl.tahlan.questgiver.wispLib.findFirst

/**
 * Return to initial point of contact.
 * Cieve thanks again and promises to contact player again should she need “some muscle” again in the future
 */
class DebriefWithCieveDialog(
        mission: NXAHubMission = game.sector.intelManager.findFirst()!!
) : InteractionDialogLogic<DebriefWithCieveDialog>(
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

                                            mission.setCurrentStage(NXAHubMission.Stage.Completed, dialog, null)
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